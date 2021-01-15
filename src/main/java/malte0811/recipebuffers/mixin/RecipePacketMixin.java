package malte0811.recipebuffers.mixin;

import io.netty.channel.ChannelHandlerContext;
import malte0811.recipebuffers.impl.NewRecipePacket;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;

@Mixin(SUpdateRecipesPacket.class)
public class RecipePacketMixin {
    @Shadow
    private List<IRecipe<?>> recipes;

    /**
     * The other "side" of this is handled in {@link PacketDecoderMixin#redirectReadPacketData(IPacket, PacketBuffer, ChannelHandlerContext)}
     *
     * @reason Use more efficient format to send recipes over the network
     * @author malte0811
     */
    @Overwrite
    public void writePacketData(PacketBuffer buf) throws IOException {
        NewRecipePacket.writePacketData(buf, recipes);
    }

    @Inject(method = "processPacket", at = @At("HEAD"))
    public void processPacketHead(IClientPlayNetHandler handler, CallbackInfo ci) {
        NewRecipePacket.processPacketHead((SUpdateRecipesPacket) (Object) this);
    }
}
