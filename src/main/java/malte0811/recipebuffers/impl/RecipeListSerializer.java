package malte0811.recipebuffers.impl;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBufUtil;
import malte0811.recipebuffers.Config;
import malte0811.recipebuffers.RecipeBuffers;
import malte0811.recipebuffers.util.IngredientSerializer;
import malte0811.recipebuffers.util.StateStack;
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
        final boolean writeLength = Config.writeRecipeLength.get();
        byte configByte = 0;
        if (writeLength) {
            configByte |= ConfigMasks.RECIPE_LENGTH;
        }
        bufIn.writeByte(configByte);

        final StateStack.Entry computeLists = StateStack.push("Compute recipe lists");
        OptimizedPacketBuffer buf = new OptimizedPacketBuffer(bufIn, false);
        Map<IRecipeSerializer<?>, List<IRecipe<?>>> bySerializer = new IdentityHashMap<>();
        for (IRecipe<?> recipe : recipes) {
            bySerializer.computeIfAbsent(recipe.getSerializer(), ser -> new ArrayList<>())
                    .add(recipe);
        }
        computeLists.pop();

        IngredientSerializer ingredientSerializer = new IngredientSerializer(buf, false);
        buf.writeVarInt(bySerializer.size());
        if (debugLevel > 0) {
            WRITE_LOGGER.debug("Number of serializers: {}", bySerializer.size());
        }
        for (Map.Entry<IRecipeSerializer<?>, List<IRecipe<?>>> entry : bySerializer.entrySet()) {
            final StateStack.Entry serializerEntry = StateStack.push(
                    "Writing recipes for serializer " + entry.getKey().getRegistryName()
            );
            if (debugLevel > 0) {
                WRITE_LOGGER.debug(
                        "Writing {} recipes for serializer {}",
                        entry.getValue().size(), entry.getKey().getRegistryName()
                );
            }
            buf.writeRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS, entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (IRecipe<?> recipe : entry.getValue()) {
                final StateStack.Entry recipeEntry = StateStack.push(
                        "Writing recipe " + recipe.getId()
                );
                if (debugLevel > 1) {
                    WRITE_LOGGER.debug("Writing recipe {} (name: {})", recipe, recipe.getId());
                }
                final int oldWriteIndex = buf.writerIndex();
                writeRecipe(recipe, buf, entry.getKey(), ingredientSerializer, writeLength);
                if (debugLevel > 1) {
                    WRITE_LOGGER.debug("Wrote recipe, takes {} bytes", buf.writerIndex() - oldWriteIndex);
                }
                recipeEntry.pop();
            }
            serializerEntry.pop();
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

    public static List<IRecipe<?>> readRecipes(PacketBuffer bufIn) throws IOException {
        if (Config.dumpPacket.get()) {
            try (FileOutputStream out = new FileOutputStream("read_recipes.dmp")) {
                out.write(ByteBufUtil.getBytes(bufIn.copy()));
            }
        }
        final int debugLevel = Config.debugLogLevel.get();
        OptimizedPacketBuffer buf = new OptimizedPacketBuffer(bufIn, true);
        final byte configByte = buf.readByte();
        final boolean includesLengthPrefix = (configByte & ConfigMasks.RECIPE_LENGTH) != 0;
        List<IRecipe<?>> recipes = Lists.newArrayList();
        IngredientSerializer ingredientSerializer = new IngredientSerializer(buf, true);
        int numSerializer = buf.readVarInt();
        if (debugLevel > 0) {
            READ_LOGGER.debug("Number of serializers: {}", numSerializer);
        }
        for (int serId = 0; serId < numSerializer; ++serId) {
            IRecipeSerializer<?> serializer = buf.readRegistryIdUnsafe(ForgeRegistries.RECIPE_SERIALIZERS);
            final StateStack.Entry serializerEntry = StateStack.push(
                    "Reading recipes for serializer " + serializer.getRegistryName()
            );
            int numRecipes = buf.readVarInt();
            if (debugLevel > 0) {
                READ_LOGGER.debug("Reading {} recipes for serializer {}", numRecipes, serializer.getRegistryName());
            }

            for (int recId = 0; recId < numRecipes; ++recId) {
                final int oldReadIndex = buf.readerIndex();
                IRecipe<?> readRecipe = readRecipe(buf, serializer, ingredientSerializer, includesLengthPrefix);
                recipes.add(readRecipe);
                if (debugLevel > 1) {
                    READ_LOGGER.debug(
                            "Read recipe {} (name {}), bytes read: {}",
                            readRecipe, readRecipe.getId(), buf.readerIndex() - oldReadIndex
                    );
                }
            }
            serializerEntry.pop();
        }
        return recipes;
    }

    public static <R extends IRecipe<?>> R readRecipe(
            OptimizedPacketBuffer buffer,
            IRecipeSerializer<R> serializer,
            IngredientSerializer ingredientSerializer,
            boolean readLength
    ) {
        final int expectedLength;
        if (readLength) {
            expectedLength = buffer.readVarInt();
        } else {
            expectedLength = -1;
        }
        final int oldReadIndex = buffer.readerIndex();
        ResourceLocation name = buffer.readResourceLocation();
        StateStack.Entry recipeEntry = StateStack.push("Reading recipe " + name);
        R result;
        if (serializer instanceof IRecurringRecipeSerializer<?>)
            result = ((IRecurringRecipeSerializer<R>) serializer).read(name, buffer, ingredientSerializer);
        else
            result = serializer.read(name, buffer);
        final int recipeLength = buffer.readerIndex() - oldReadIndex;
        if (readLength && expectedLength != recipeLength) {
            throw new IllegalStateException(
                    "Recipe read " + recipeLength + " bytes, but wrote " + expectedLength + " bytes, see log for details"
            );
        } else {
            recipeEntry.pop();
            return result;
        }
    }

    public static <T extends IRecipe<?>> void writeRecipe(
            T recipe,
            OptimizedPacketBuffer buffer,
            IRecipeSerializer<?> serializer,
            IngredientSerializer ingredientSerializer,
            boolean writeLength
    ) {
        final int writerIndexBefore = buffer.writerIndex();
        buffer.writeResourceLocation(recipe.getId());
        if (serializer instanceof IRecurringRecipeSerializer<?>)
            ((IRecurringRecipeSerializer<T>) serializer).write(buffer, recipe, ingredientSerializer);
        else
            ((IRecipeSerializer<T>) serializer).write(buffer, recipe);
        if (writeLength) {
            final int readerIndexBefore = buffer.readerIndex();
            final int bytesWritten = buffer.writerIndex() - writerIndexBefore;
            buffer.readerIndex(writerIndexBefore);
            byte[] recipeBytes = new byte[bytesWritten];
            buffer.readBytes(recipeBytes);
            buffer.readerIndex(readerIndexBefore);

            buffer.writerIndex(writerIndexBefore);
            buffer.writeVarInt(bytesWritten);
            buffer.writeBytes(recipeBytes);
        }
    }

    private static class ConfigMasks {
        public static final byte RECIPE_LENGTH = 1;
    }
}
