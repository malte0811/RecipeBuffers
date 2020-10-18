package malte0811.recipebuffers.impl;

import malte0811.recipebuffers.RecipeBuffers;
import malte0811.recipebuffers.util.IngredientSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

// Uses recurring data optimization, very effective on recipes including large tag inputs
public class RecurringShapedSerializer extends ShapedRecipe.Serializer {
    public static final RecurringShapedSerializer INSTANCE = new RecurringShapedSerializer();

    {
        setRegistryName(RecipeBuffers.MODID, "shaped");
    }

    @Override
    public void write(PacketBuffer buffer, ShapedRecipe recipe) {
        buffer.writeVarInt(recipe.getRecipeWidth());
        buffer.writeVarInt(recipe.getRecipeHeight());
        buffer.writeString(recipe.getGroup());

        IngredientSerializer serializer = new IngredientSerializer(buffer, false);
        for(Ingredient ingredient : recipe.getIngredients()) {
            serializer.write(ingredient);
        }

        buffer.writeItemStack(recipe.getRecipeOutput());
    }

    @Override
    public ShapedRecipe read(@Nonnull ResourceLocation recipeId, PacketBuffer buffer) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        String group = buffer.readString(32767);
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        IngredientSerializer serializer = new IngredientSerializer(buffer, true);
        for (int i = 0; i < ingredients.size(); ++i) {
            ingredients.set(i, serializer.read());
        }

        ItemStack output = buffer.readItemStack();
        return new ShapedRecipe(recipeId, group, width, height, ingredients, output);
    }
}
