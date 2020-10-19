package malte0811.recipebuffers.impl;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBufUtil;
import malte0811.recipebuffers.RecipeBuffers;
import malte0811.recipebuffers.util.IngredientSerializer;
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

/**
 * Differences from vanilla implementation:
 * - Uses {@link OptimizedPacketBuffer} for everything
 * - Does not send the serializer ID for every recipe, instead sorts by ID and sends each ID once
 */
public class RecipeListSerializer {
    public static void writeRecipes(List<IRecipe<?>> recipes, PacketBuffer bufIn) throws IOException {
        OptimizedPacketBuffer buf = new OptimizedPacketBuffer(bufIn, false);
        Map<IRecipeSerializer<?>, List<IRecipe<?>>> bySerializer = new IdentityHashMap<>();
        for (IRecipe<?> recipe : recipes) {
            bySerializer.computeIfAbsent(recipe.getSerializer(), ser -> new ArrayList<>())
                    .add(recipe);
        }

        IngredientSerializer ingredientSerializer = new IngredientSerializer(buf, false);
        buf.writeVarInt(bySerializer.size());
        for (Map.Entry<IRecipeSerializer<?>, List<IRecipe<?>>> entry : bySerializer.entrySet()) {
            buf.writeRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS, entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (IRecipe<?> recipe : entry.getValue()) {
                writeRecipe(recipe, buf, entry.getKey(), ingredientSerializer);
            }
        }

        //Debug code
        try (FileOutputStream out = new FileOutputStream("recipes.dmp")) {
            out.write(ByteBufUtil.getBytes(buf.copy()));
        }
        RecipeBuffers.LOGGER.info("Recipe packet size: {}", buf.readableBytes());
        RecipeBuffers.LOGGER.info("Item stack bytes: {}", buf.itemStackBytes);
        RecipeBuffers.LOGGER.info("RL path bytes: {}", buf.rlPathBytes);
        RecipeBuffers.LOGGER.info("Ingredient cache size: {}", ingredientSerializer.cacheSize());
        RecipeBuffers.LOGGER.info("Ingredient cache hits: {}", ingredientSerializer.cacheHits());
    }

    public static List<IRecipe<?>> readRecipes(PacketBuffer buf) {
        buf = new OptimizedPacketBuffer(buf, true);
        List<IRecipe<?>> recipes = Lists.newArrayList();
        IngredientSerializer ingredientSerializer = new IngredientSerializer(buf, true);
        int numSerializer = buf.readVarInt();
        for (int serId = 0; serId < numSerializer; ++serId) {
            IRecipeSerializer<?> serializer = buf.readRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS);
            int numRecipes = buf.readVarInt();

            for (int recId = 0; recId < numRecipes; ++recId) {
                recipes.add(readRecipe(buf, serializer, ingredientSerializer));
            }
        }
        return recipes;
    }

    public static <R extends IRecipe<?>> R readRecipe(
            PacketBuffer buffer,
            IRecipeSerializer<R> serializer,
            IngredientSerializer ingredientSerializer
    ) {
        ResourceLocation name = buffer.readResourceLocation();
        if (serializer instanceof IRecurringRecipeSerializer<?>)
            return ((IRecurringRecipeSerializer<R>) serializer).read(name, buffer, ingredientSerializer);
        else
            return serializer.read(name, buffer);
    }

    public static <T extends IRecipe<?>> void writeRecipe(
            T recipe,
            PacketBuffer buffer,
            IRecipeSerializer<?> serializer,
            IngredientSerializer ingredientSerializer
    ) {
        buffer.writeResourceLocation(recipe.getId());
        if (serializer instanceof IRecurringRecipeSerializer<?>)
            ((IRecurringRecipeSerializer<T>) serializer).write(buffer, recipe, ingredientSerializer);
        else
            ((IRecipeSerializer<T>) serializer).write(buffer, recipe);
    }
}
