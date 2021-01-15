package malte0811.recipebuffers.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import malte0811.recipebuffers.impl.NewRecipePacket;
import net.minecraft.network.NettyPacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NettyPacketDecoder.class)
public class PacketDecoderMixin {
    @Inject(method = "decode", at = @At("HEAD"))
    public void decodeHead(
            ChannelHandlerContext p_decode_1_, ByteBuf p_decode_2_, List<Object> p_decode_3_, CallbackInfo ci
    ) {
        NewRecipePacket.processPacketPre(p_decode_1_);
    }
}
