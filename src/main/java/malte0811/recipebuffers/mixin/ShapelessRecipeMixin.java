package malte0811.recipebuffers.mixin;

import malte0811.recipebuffers.impl.RecurringShapelessSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ShapelessRecipe.class)
public class ShapelessRecipeMixin {
    /**
     * Use custom serializer for more efficient serialization of duplicate ingredients
     * @author malte0811
     */
    @Overwrite
    public IRecipeSerializer<?> getSerializer() {
        return RecurringShapelessSerializer.INSTANCE;
    }
}
