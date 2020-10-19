package malte0811.recipebuffers.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class IngredientSerializer {
    private final PacketBuffer buffer;
    private final RecurringData<Ingredient> basicIngredientHandler;

    public IngredientSerializer(PacketBuffer buffer, boolean reading) {
        this.buffer = buffer;
        this.basicIngredientHandler = createRecurringData(buffer, reading);
    }

    private static RecurringData<Ingredient> createRecurringData(PacketBuffer buffer, boolean reader) {
        return RecurringData.createForList(
                buffer,
                b -> new ItemstackWrapper(b.readItemStack()),
                (b, i) -> b.writeItemStack(i.stack),
                reader
        ).xmap(
                l -> {
                    ItemStack[] stacks = l.stream().map(w -> w.stack).toArray(ItemStack[]::new);
                    return Ingredient.fromStacks(stacks);
                },
                i -> Arrays.stream(i.getMatchingStacks()).map(ItemstackWrapper::new).collect(Collectors.toList())
        );
    }

    public <T extends Ingredient> void write(T ingredient) {
        boolean isSpecial = !ingredient.isVanilla();
        //TODO this bool should ideally be part of main VarInt
        buffer.writeBoolean(isSpecial);
        if (isSpecial) {
            @SuppressWarnings("unchecked")
            IIngredientSerializer<T> serializer = (IIngredientSerializer<T>) ingredient.getSerializer();
            ResourceLocation key = CraftingHelper.getID(serializer);
            if (key == null)
                throw new IllegalArgumentException("Tried to serialize unregistered Ingredient: " + ingredient + " " + serializer);
            buffer.writeResourceLocation(key);
            serializer.write(buffer, ingredient);
        } else {
            basicIngredientHandler.write(ingredient);
        }
    }

    public Ingredient read() {
        if (buffer.readBoolean()) {
            return CraftingHelper.getIngredient(buffer.readResourceLocation(), buffer);
        } else {
            return basicIngredientHandler.read();
        }
    }

    public int cacheSize() {
        return basicIngredientHandler.size();
    }

    public int cacheHits() {
        return basicIngredientHandler.hits();
    }

    private static class ItemstackWrapper {
        private final ItemStack stack;

        private ItemstackWrapper(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemstackWrapper that = (ItemstackWrapper) o;
            return ItemStack.areItemStacksEqual(stack, that.stack);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stack.getItem(), stack.getCount(), stack.getTag());
        }
    }
}
