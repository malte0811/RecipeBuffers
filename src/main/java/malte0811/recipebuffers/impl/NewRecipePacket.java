package malte0811.recipebuffers.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import malte0811.recipebuffers.Config;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateRecipesPacket;

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

    public static void processPacketHead(SUpdateRecipesPacket packet) {
        if (Config.runSerializerInSingleplayer.get()) {
            ByteBuf buffer = Unpooled.buffer();
            PacketBuffer temp = new PacketBuffer(buffer);
            try {
                packet.writePacketData(temp);
                packet.readPacketData(temp);
            } catch (Throwable x) {
                throw new RuntimeException(x);
            }
        }
    }
}
