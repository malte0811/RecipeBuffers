package malte0811.recipebuffers.impl;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.List;

public class NewRecipePacket {
    public static List<IRecipe<?>> readPacketData(PacketBuffer buf) throws IOException {
        int initialIndex = buf.readerIndex();
        try {
            return RecipeListSerializer.readRecipes(buf);
        } catch (Throwable x) {
            ErrorLogger.logReadError(x, buf, initialIndex);
            throw x;
        }
    }

    public static void writePacketData(PacketBuffer buf, List<IRecipe<?>> recipes) throws IOException {
        int initialIndex = buf.writerIndex();
        try {
            RecipeListSerializer.writeRecipes(recipes, buf);
        } catch (Throwable x) {
            ErrorLogger.logWriteError(x, buf, initialIndex);
            throw x;
        }
    }
}
