package malte0811.recipebuffers.impl;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class OptimizedPacketBuffer extends PacketBuffer {
    private final boolean reading;
    private final List<String> knownNamespaces = new ArrayList<>();

    public OptimizedPacketBuffer(ByteBuf wrapped, boolean reading) {
        super(wrapped);
        this.reading = reading;
    }

    @Nonnull
    @Override
    public ResourceLocation readResourceLocation() {
        Preconditions.checkState(reading);
        int id = readVarInt() - 1;
        String namespace;
        if (id < 0) {
            namespace = readString();
            knownNamespaces.add(namespace);
        } else {
            namespace = knownNamespaces.get(id);
        }
        return new ResourceLocation(namespace, readString());
    }

    @Nonnull
    @Override
    public PacketBuffer writeResourceLocation(@Nonnull ResourceLocation toWrite) {
        Preconditions.checkState(!reading);
        int id = knownNamespaces.indexOf(toWrite.getNamespace());
        writeVarInt(id + 1);
        if (id < 0) {
            writeString(toWrite.getNamespace());
            knownNamespaces.add(toWrite.getNamespace());
        }
        writeString(toWrite.getPath());
        return this;
    }

    @Nonnull
    @Override
    public PacketBuffer writeItemStack(ItemStack stack, boolean limitedTag) {
        Item item = stack.getItem();
        writeRegistryIdUnsafe(ForgeRegistries.ITEMS, item);
        if (!stack.isEmpty()) {
            this.writeByte(stack.getCount());
            CompoundNBT compoundnbt = null;
            if ((item.isDamageable(stack) || item.shouldSyncTag())
                    //TODO faster/better check?
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
