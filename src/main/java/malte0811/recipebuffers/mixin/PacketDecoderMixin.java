package malte0811.recipebuffers.mixin;

import io.netty.channel.ChannelHandlerContext;
import malte0811.recipebuffers.impl.NewRecipePacket;
import net.minecraft.network.IPacket;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;

@Mixin(NettyPacketDecoder.class)
public class PacketDecoderMixin {
    @Redirect(
            method = "decode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/IPacket;readPacketData(Lnet/minecraft/network/PacketBuffer;)V"
            )
    )
    public void redirectReadPacketData(
            IPacket<?> iPacket, PacketBuffer buf, ChannelHandlerContext context
    ) throws IOException {
        NewRecipePacket.processAnyPacketRead(iPacket, buf, context);
    }
}
