package malte0811.recipebuffers.mixin;

import malte0811.recipebuffers.impl.RecurringShapedSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    /**
     * Use custom serializer for more efficient serialization of duplicate ingredients
     * @author malte0811
     */
    @Overwrite
    public IRecipeSerializer<?> getSerializer() {
        return RecurringShapedSerializer.INSTANCE;
    }
}
