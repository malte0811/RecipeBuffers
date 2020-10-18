package malte0811.recipebuffers;

import malte0811.recipebuffers.impl.RecurringShapedSerializer;
import malte0811.recipebuffers.impl.RecurringShapelessSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RecipeBuffers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RBRecipeSerializers {
    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> ev) {
        ev.getRegistry().register(RecurringShapelessSerializer.INSTANCE);
        ev.getRegistry().register(RecurringShapedSerializer.INSTANCE);
    }
}
