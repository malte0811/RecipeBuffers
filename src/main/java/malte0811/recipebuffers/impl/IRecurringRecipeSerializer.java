package malte0811.recipebuffers.impl;

import malte0811.recipebuffers.util.IngredientSerializer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IRecurringRecipeSerializer<R extends IRecipe<?>> extends IRecipeSerializer<R> {
    @Nullable
    @Override
    default R read(@Nonnull ResourceLocation recipeId, @Nonnull PacketBuffer buffer) {
        return read(recipeId, buffer, new IngredientSerializer(buffer, true));
    }

    @Override
    default void write(@Nonnull PacketBuffer buffer, @Nonnull R recipe) {
        write(buffer, recipe, new IngredientSerializer(buffer, false));
    }

    R read(@Nonnull ResourceLocation recipeId, @Nonnull PacketBuffer buffer, @Nonnull IngredientSerializer ingredients);

    void write(@Nonnull PacketBuffer buffer, @Nonnull R recipe, @Nonnull IngredientSerializer ingredients);
}
