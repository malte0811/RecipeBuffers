package malte0811.recipebuffers.impl;

import io.netty.buffer.ByteBuf;
import malte0811.recipebuffers.util.RecurringData;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

public class OptimizedPacketBuffer extends PacketBuffer {
    private final RecurringData<String> namespaces;

    public OptimizedPacketBuffer(ByteBuf wrapped, boolean reading) {
        super(wrapped);
        this.namespaces = RecurringData.create(
                this, PacketBuffer::readString, String::equals, PacketBuffer::writeString, reading
        );
    }

    @Nonnull
    @Override
    public ResourceLocation readResourceLocation() {
        return new ResourceLocation(namespaces.read(), readString());
    }

    @Nonnull
    @Override
    public PacketBuffer writeResourceLocation(@Nonnull ResourceLocation toWrite) {
        namespaces.write(toWrite.getNamespace());
        writeString(toWrite.getPath());
        return this;
    }

    @Nonnull
    @Override
    public PacketBuffer writeItemStack(ItemStack stack, boolean limitedTag) {
        Item item = stack.getItem();
        // Do not send empty/nonempty as an extra byte, but use the fact that getItem()==AIR iff empty
        writeRegistryIdUnsafe(ForgeRegistries.ITEMS, item);
        if (!stack.isEmpty()) {
            this.writeByte(stack.getCount());
            CompoundNBT compoundnbt = null;
            if ((item.isDamageable(stack) || item.shouldSyncTag())
                    // Damageable items always have an NBT tag, which is larger (when serialized) all the rest of the
                    // item. This skips the tag if it's the one automatically generated in the constructor.
                    && !ItemStack.areItemStackTagsEqual(stack, new ItemStack(item))) {
                compoundnbt = limitedTag ? stack.getShareTag() : stack.getTag();
            }

            this.writeCompoundTag(compoundnbt);
        }

        return this;
    }

    @Nonnull
    @Override
    public ItemStack readItemStack() {
        Item item = readRegistryIdUnsafe(ForgeRegistries.ITEMS);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        } else {
            int count = this.readByte();
            ItemStack itemstack = new ItemStack(item, count);
            itemstack.readShareTag(this.readCompoundTag());
            return itemstack;
        }
    }
}
