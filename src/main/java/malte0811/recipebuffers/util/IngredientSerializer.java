package malte0811.recipebuffers.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

import java.util.Arrays;

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
                PacketBuffer::readItemStack,
                ItemStack::areItemStacksEqual,
                PacketBuffer::writeItemStack,
                reader
        ).xmap(
                l -> Ingredient.fromStacks(l.toArray(new ItemStack[0])),
                i -> Arrays.asList(i.getMatchingStacks())
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
}
