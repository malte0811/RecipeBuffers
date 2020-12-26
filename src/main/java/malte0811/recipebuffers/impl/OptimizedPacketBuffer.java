package malte0811.recipebuffers.impl;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTTypes;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

public class OptimizedPacketBuffer extends PacketBuffer {
    int itemStackBytes = 0;
    final ResourceLocationSerializer resourceLocationSerializer;

    public OptimizedPacketBuffer(ByteBuf wrapped, boolean reading) {
        super(wrapped);
        resourceLocationSerializer = new ResourceLocationSerializer(this, reading);
    }

    @Nonnull
    @Override
    public ResourceLocation readResourceLocation() {
        return resourceLocationSerializer.read();
    }

    @Nonnull
    @Override
    public PacketBuffer writeResourceLocation(@Nonnull ResourceLocation toWrite) {
        resourceLocationSerializer.write(toWrite);
        return this;
    }

    private static final int CONSTRUCTOR_TAG = 13;
    static {
        String tagName = NBTTypes.getGetTypeByID(CONSTRUCTOR_TAG).getTagName();
        Preconditions.checkState(tagName.startsWith("UNKNOWN_"), "Unexpected NBT type: "+tagName);
    }

    @Nonnull
    @Override
    public PacketBuffer writeItemStack(ItemStack stack, boolean limitedTag) {
        final int oldIndex = writerIndex();
        Item item = stack.getItem();
        // Do not send empty/nonempty as an extra byte, but use the fact that getItem()==AIR iff empty
        writeRegistryIdUnsafe(ForgeRegistries.ITEMS, item);
        if (!stack.isEmpty()) {
            this.writeByte(stack.getCount());
            // Damageable items always have an NBT tag, which is larger (when serialized) all the rest of the
            // item. This skips the tag if it's the one automatically generated in the constructor.
            if (ItemStack.areItemStackTagsEqual(stack, new ItemStack(item))) {
                this.writeByte(CONSTRUCTOR_TAG);
            } else {
                CompoundNBT compoundnbt = null;
                if (item.isDamageable(stack) || item.shouldSyncTag()) {
                    compoundnbt = limitedTag ? stack.getShareTag() : stack.getTag();
                }
                this.writeCompoundTag(compoundnbt);
            }

        }
        itemStackBytes += writerIndex() - oldIndex;

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
            final int oldIndex = this.readerIndex();
            int tagByte = readByte();
            CompoundNBT tag;
            if (tagByte == CONSTRUCTOR_TAG) {
                tag = itemstack.getTag();
            } else {
                readerIndex(oldIndex);
                tag = this.readCompoundTag();
            }
            itemstack.readShareTag(tag);
            return itemstack;
        }
    }
}
