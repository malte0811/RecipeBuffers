package malte0811.recipebuffers.mixin;

import malte0811.recipebuffers.impl.RecipeListSerializer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.List;

@Mixin(SUpdateRecipesPacket.class)
public class RecipePacketMixin {
    @Shadow
    private List<IRecipe<?>> recipes;

    /**
     * Use more efficient format to send recipes over the network
     *
     * @author malte0811
     */
    @Overwrite
    public void readPacketData(PacketBuffer buf) throws IOException {
        recipes = RecipeListSerializer.readRecipes(buf);
    }

    /**
     * Use more efficient format to send recipes over the network
     *
     * @author malte0811
     */
    @Overwrite
    public void writePacketData(PacketBuffer buf) throws IOException {
        RecipeListSerializer.writeRecipes(recipes, buf);
    }

}
