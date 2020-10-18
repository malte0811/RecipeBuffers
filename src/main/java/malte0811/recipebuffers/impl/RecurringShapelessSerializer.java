package malte0811.recipebuffers.impl;

import malte0811.recipebuffers.RecipeBuffers;
import malte0811.recipebuffers.util.IngredientSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class RecurringShapelessSerializer extends ShapelessRecipe.Serializer {
    public static final RecurringShapelessSerializer INSTANCE = new RecurringShapelessSerializer();

    {
        setRegistryName(RecipeBuffers.MODID, "shapeless");
    }

    @Override
    public void write(@Nonnull PacketBuffer buffer, @Nonnull ShapelessRecipe recipe) {
        buffer.writeString(recipe.getGroup());
        buffer.writeVarInt(recipe.getIngredients().size());

        IngredientSerializer serializer = new IngredientSerializer(buffer, false);
        for(Ingredient ingredient : recipe.getIngredients()) {
            serializer.write(ingredient);
        }

        buffer.writeItemStack(recipe.getRecipeOutput());
    }

    @Override
    public ShapelessRecipe read(@Nonnull ResourceLocation recipeId, @Nonnull PacketBuffer buffer) {
        String group = buffer.readString(32767);
        int numIngredients = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(numIngredients, Ingredient.EMPTY);

        IngredientSerializer serializer = new IngredientSerializer(buffer, true);
        for(int j = 0; j < ingredients.size(); ++j) {
            ingredients.set(j, serializer.read());
        }

        ItemStack result = buffer.readItemStack();
        return new ShapelessRecipe(recipeId, group, result, ingredients);
    }
}
