package malte0811.recipebuffers.impl;

import malte0811.recipebuffers.util.IngredientSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

// Uses recurring data optimization, very effective on recipes including large tag inputs
public class RecurringShapelessSerializer extends ShapelessRecipe.Serializer implements IRecurringRecipeSerializer<ShapelessRecipe> {
    @Override
    public void write(
            @Nonnull PacketBuffer buffer, @Nonnull ShapelessRecipe recipe, @Nonnull IngredientSerializer ingredients
    ) {
        buffer.writeString(recipe.getGroup());
        buffer.writeVarInt(recipe.getIngredients().size());

        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredients.write(ingredient);
        }

        buffer.writeItemStack(recipe.getRecipeOutput());
    }

    @Override
    public ShapelessRecipe read(
            @Nonnull ResourceLocation recipeId, @Nonnull PacketBuffer buffer, @Nonnull IngredientSerializer ingredients
    ) {
        String group = buffer.readString(32767);
        int numIngredients = buffer.readVarInt();
        NonNullList<Ingredient> ingredientList = NonNullList.withSize(numIngredients, Ingredient.EMPTY);

        for (int j = 0; j < ingredientList.size(); ++j) {
            ingredientList.set(j, ingredients.read());
        }

        ItemStack result = buffer.readItemStack();
        return new ShapelessRecipe(recipeId, group, result, ingredientList);
    }
}
