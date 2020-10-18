package malte0811.recipebuffers.impl;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBufUtil;
import malte0811.recipebuffers.RecipeBuffers;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class RecipeListSerializer {
    public static void writeRecipes(List<IRecipe<?>> recipes, PacketBuffer buf) throws IOException {
        buf = new OptimizedPacketBuffer(buf, false);
        Map<IRecipeSerializer<?>, List<IRecipe<?>>> bySerializer = new IdentityHashMap<>();
        for (IRecipe<?> recipe : recipes) {
            bySerializer.computeIfAbsent(recipe.getSerializer(), ser -> new ArrayList<>())
                    .add(recipe);
        }

        buf.writeVarInt(bySerializer.size());
        for (Map.Entry<IRecipeSerializer<?>, List<IRecipe<?>>> entry : bySerializer.entrySet()) {
            buf.writeRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS, entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (IRecipe<?> recipe : entry.getValue()) {
                func_218771_a(recipe, buf, entry.getKey());
            }
        }

        //Debug code
        try (FileOutputStream out = new FileOutputStream("recipes.dmp")) {
            out.write(ByteBufUtil.getBytes(buf.copy()));
        }
        RecipeBuffers.LOGGER.info("Buffer size: {}", buf.readableBytes());
    }

    public static List<IRecipe<?>> readRecipes(PacketBuffer buf) {
        buf = new OptimizedPacketBuffer(buf, true);
        List<IRecipe<?>> recipes = Lists.newArrayList();
        int numSerializer = buf.readVarInt();
        for (int serId = 0; serId < numSerializer; ++serId) {
            IRecipeSerializer<?> serializer = buf.readRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS);
            int numRecipes = buf.readVarInt();

            for (int recId = 0; recId < numRecipes; ++recId) {
                recipes.add(readRecipe(buf, serializer));
            }
        }
        return recipes;
    }

    public static <R extends IRecipe<?>> R readRecipe(PacketBuffer buffer, IRecipeSerializer<R> serializer) {
        ResourceLocation name = buffer.readResourceLocation();
        return serializer.read(name, buffer);
    }

    public static <T extends IRecipe<?>> void func_218771_a(T p_218771_0_, PacketBuffer p_218771_1_, IRecipeSerializer<?> serializer) {
        p_218771_1_.writeResourceLocation(p_218771_0_.getId());
        ((net.minecraft.item.crafting.IRecipeSerializer<T>)serializer).write(p_218771_1_, p_218771_0_);
    }
}
