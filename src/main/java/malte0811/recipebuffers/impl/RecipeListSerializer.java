package malte0811.recipebuffers.impl;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBufUtil;
import malte0811.recipebuffers.Config;
import malte0811.recipebuffers.RecipeBuffers;
import malte0811.recipebuffers.util.IngredientSerializer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public static final Logger WRITE_LOGGER = LogManager.getLogger(RecipeBuffers.MODID + " - WRITE");
    public static final Logger READ_LOGGER = LogManager.getLogger(RecipeBuffers.MODID + " - READ");

    public static void writeRecipes(List<IRecipe<?>> recipes, PacketBuffer bufIn) throws IOException {
        final int debugLevel = Config.debugLogLevel.get();
        OptimizedPacketBuffer buf = new OptimizedPacketBuffer(bufIn, false);
        Map<IRecipeSerializer<?>, List<IRecipe<?>>> bySerializer = new IdentityHashMap<>();
        for (IRecipe<?> recipe : recipes) {
            bySerializer.computeIfAbsent(recipe.getSerializer(), ser -> new ArrayList<>())
                    .add(recipe);
        }

        IngredientSerializer ingredientSerializer = new IngredientSerializer(buf, false);
        buf.writeVarInt(bySerializer.size());
        if (debugLevel > 0) {
            WRITE_LOGGER.debug("Number of serializers: {}", bySerializer.size());
        }
        for (Map.Entry<IRecipeSerializer<?>, List<IRecipe<?>>> entry : bySerializer.entrySet()) {
            if (debugLevel > 0) {
                WRITE_LOGGER.debug(
                        "Writing {} recipes for serializer {}",
                        entry.getValue().size(), entry.getKey().getRegistryName()
                );
            }
            buf.writeRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS, entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (IRecipe<?> recipe : entry.getValue()) {
                if (debugLevel > 1) {
                    WRITE_LOGGER.debug("Writing recipe {} (name: {})", recipe, recipe.getId());
                }
                final int oldWriteIndex = buf.writerIndex();
                writeRecipe(recipe, buf, entry.getKey(), ingredientSerializer);
                if (debugLevel > 1) {
                    WRITE_LOGGER.debug("Wrote recipe, takes {} bytes", buf.writerIndex() - oldWriteIndex);
                }
            }
        }

        if (Config.dumpPacket.get()) {
            try (FileOutputStream out = new FileOutputStream("written_recipes.dmp")) {
                out.write(ByteBufUtil.getBytes(buf.copy()));
            }
        }
        if (Config.logPacketStats.get()) {
            WRITE_LOGGER.info("Recipe packet size: {}", buf.readableBytes());
            WRITE_LOGGER.info("Item stack bytes: {}", buf.itemStackBytes);
            WRITE_LOGGER.info("RL path bytes: {}", buf.rlPathBytes);
            WRITE_LOGGER.info("Ingredient cache size: {}", ingredientSerializer.cacheSize());
            WRITE_LOGGER.info("Ingredient cache hits: {}", ingredientSerializer.cacheHits());
        }
    }

    public static List<IRecipe<?>> readRecipes(PacketBuffer buf) throws IOException {
        if (Config.dumpPacket.get()) {
            try (FileOutputStream out = new FileOutputStream("read_recipes.dmp")) {
                out.write(ByteBufUtil.getBytes(buf.copy()));
            }
        }
        final int debugLevel = Config.debugLogLevel.get();
        buf = new OptimizedPacketBuffer(buf, true);
        List<IRecipe<?>> recipes = Lists.newArrayList();
        IngredientSerializer ingredientSerializer = new IngredientSerializer(buf, true);
        int numSerializer = buf.readVarInt();
        if (debugLevel > 0) {
            READ_LOGGER.debug("Number of serializers: {}", numSerializer);
        }
        for (int serId = 0; serId < numSerializer; ++serId) {
            IRecipeSerializer<?> serializer = buf.readRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS);
            int numRecipes = buf.readVarInt();
            if (debugLevel > 0) {
                READ_LOGGER.debug("Reading {} recipes for serializer {}", numRecipes, serializer.getRegistryName());
            }

            for (int recId = 0; recId < numRecipes; ++recId) {
                final int oldReadIndex = buf.readerIndex();
                IRecipe<?> readRecipe = readRecipe(buf, serializer, ingredientSerializer);
                recipes.add(readRecipe);
                if (debugLevel > 1) {
                    READ_LOGGER.debug(
                            "Read recipe {} (name {}), bytes read: {}",
                            readRecipe, readRecipe.getId(), buf.readerIndex() - oldReadIndex
                    );
                }
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
