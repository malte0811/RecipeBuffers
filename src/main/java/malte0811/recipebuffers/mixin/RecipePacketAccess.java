package malte0811.recipebuffers.mixin;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.server.SUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SUpdateRecipesPacket.class)
public interface RecipePacketAccess {
    @Accessor
    void setRecipes(List<IRecipe<?>> newRecipes);

    @Accessor
        //There is one in vanilla, but it's side-only CLIENT
    List<IRecipe<?>> getRecipes();
}
