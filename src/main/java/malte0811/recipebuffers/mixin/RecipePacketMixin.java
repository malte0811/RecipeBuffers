package malte0811.recipebuffers.mixin;

import malte0811.recipebuffers.impl.NewRecipePacket;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.item.crafting.IRecipe;
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
     * @reason Use more efficient format to send recipes over the network
     * @author malte0811
     */
    @Overwrite
    public void readPacketData(PacketBuffer buf) throws IOException {
        recipes = NewRecipePacket.readPacketData(buf);
    }

    /**
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
