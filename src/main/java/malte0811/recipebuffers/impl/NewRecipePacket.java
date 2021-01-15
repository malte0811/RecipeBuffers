package malte0811.recipebuffers.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import malte0811.recipebuffers.Config;
import malte0811.recipebuffers.mixin.RecipePacketAccess;
import malte0811.recipebuffers.util.StateStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateRecipesPacket;
import net.minecraftforge.fml.network.NetworkHooks;

import java.io.IOException;
import java.util.List;

public class NewRecipePacket {
    public static List<IRecipe<?>> readPacketData(PacketBuffer buf) throws IOException {
        int initialIndex = buf.readerIndex();
        try {
            StateStack.assertEmpty();
            StateStack.Entry e = StateStack.push("Reading recipes");
            List<IRecipe<?>> result = RecipeListSerializer.readRecipes(buf);
            e.pop();
            StateStack.assertEmpty();
            return result;
        } catch (Throwable x) {
            ErrorLogger.logReadError(x, buf, initialIndex);
            throw x;
        }
    }

    public static void writePacketData(PacketBuffer buf, List<IRecipe<?>> recipes) throws IOException {
        int initialIndex = buf.writerIndex();
        try {
            StateStack.assertEmpty();
            StateStack.Entry e = StateStack.push("Reading recipes");
            RecipeListSerializer.writeRecipes(recipes, buf);
            e.pop();
            StateStack.assertEmpty();
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
                RecipePacketAccess access = (RecipePacketAccess) packet;
                writePacketData(temp, access.getRecipes());
                access.setRecipes(readPacketData(temp));
            } catch (Throwable x) {
                throw new RuntimeException(x);
            }
        }
    }

    public static void processAnyPacketRead(
            IPacket<?> iPacket, PacketBuffer buf, ChannelHandlerContext context
    ) throws IOException {
        if (iPacket instanceof RecipePacketAccess && !NetworkHooks.getConnectionType(context).isVanilla()) {
            // Parse the optimized recipe format if a) we have a recipe packet and b) it's coming from a modded server
            ((RecipePacketAccess) iPacket).setRecipes(NewRecipePacket.readPacketData(buf));
        } else {
            iPacket.readPacketData(buf);
        }
    }
}
