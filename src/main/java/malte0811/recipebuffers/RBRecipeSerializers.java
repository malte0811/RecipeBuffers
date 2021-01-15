package malte0811.recipebuffers;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import malte0811.recipebuffers.impl.RecurringShapedSerializer;
import malte0811.recipebuffers.impl.RecurringShapelessSerializer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = RecipeBuffers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RBRecipeSerializers {
    private static final Map<IRecipeSerializer<?>, IRecipeSerializer<?>> REPLACEMENTS = new Object2ObjectArrayMap<>();

    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> ev) {
        RecurringShapelessSerializer recurringShapeless = new RecurringShapelessSerializer();
        recurringShapeless.setRegistryName(RecipeBuffers.MODID, "shapeless");
        ev.getRegistry().register(recurringShapeless);
        replace(IRecipeSerializer.CRAFTING_SHAPELESS, recurringShapeless);

        RecurringShapedSerializer recurringShaped = new RecurringShapedSerializer();
        recurringShaped.setRegistryName(RecipeBuffers.MODID, "shaped");
        ev.getRegistry().register(recurringShaped);
        replace(IRecipeSerializer.CRAFTING_SHAPED, recurringShaped);
    }

    private static <T extends IRecipe<?>>
    void replace(IRecipeSerializer<T> oldSer, IRecipeSerializer<T> newSer) {
        REPLACEMENTS.put(oldSer, newSer);
    }

    public static IRecipeSerializer<?> getSerializer(IRecipeSerializer<?> original) {
        return REPLACEMENTS.getOrDefault(original, original);
    }
}
