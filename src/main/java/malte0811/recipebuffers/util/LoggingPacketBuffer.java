package malte0811.recipebuffers.util;

import com.google.common.base.Preconditions;
import io.netty.buffer.Unpooled;
import malte0811.recipebuffers.impl.OptimizedPacketBuffer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LoggingPacketBuffer extends OptimizedPacketBuffer {
    private final OptimizedPacketBuffer internal;
    public final PacketBuffer log;

    public LoggingPacketBuffer(OptimizedPacketBuffer internal) {
        //TODO
        super(internal, true);
        this.internal = internal;
        this.log = new PacketBuffer(Unpooled.buffer());
    }

    @Nonnull
    public PacketBuffer writeByteArray(@Nonnull byte[] array) {
        return processWrite(Type.BYTE_ARRAY, array);
    }

    @Nonnull
    public byte[] readByteArray(int maxLength) {
        return processRead(Type.BYTE_ARRAY);
    }

    @Nonnull
    public PacketBuffer writeVarIntArray(@Nonnull int[] array) {
        return processWrite(Type.VARINT_ARRAY, array);
    }

    @Nonnull
    public int[] readVarIntArray(int maxLength) {
        return processRead(Type.VARINT_ARRAY);
    }

    @Nonnull
    public PacketBuffer writeLongArray(@Nonnull long[] array) {
        return processWrite(Type.LONG_ARRAY, array);
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public long[] readLongArray(@Nullable long[] array, int maxLength) {
        return processRead(Type.LONG_ARRAY);
    }

    @Nonnull
    public BlockPos readBlockPos() {
        return processRead(Type.BLOCK_POS);
    }

    @Nonnull
    public PacketBuffer writeBlockPos(@Nonnull BlockPos pos) {
        return processWrite(Type.BLOCK_POS, pos);
    }

    public int readVarInt() {
        return processRead(Type.VARINT);
    }

    public long readVarLong() {
        return processRead(Type.VARLONG);
    }

    @Nonnull
    public PacketBuffer writeUniqueId(@Nonnull UUID uuid) {
        return processWrite(Type.UUID, uuid);
    }

    @Nonnull
    public UUID readUniqueId() {
        return processRead(Type.UUID);
    }

    @Nonnull
    public PacketBuffer writeVarInt(int input) {
        return processWrite(Type.VARINT, input);
    }

    @Nonnull
    public PacketBuffer writeVarLong(long value) {
        return processWrite(Type.VARLONG, value);
    }

    @Nonnull
    public PacketBuffer writeCompoundTag(@Nullable CompoundNBT nbt) {
        return processWrite(Type.COMPOUND_TAG, nbt);
    }

    @Nullable
    @Override
    public CompoundNBT readCompoundTag() {
        return processRead(Type.COMPOUND_TAG);
    }

    @Nonnull
    public String readString(int maxLength) {
        return processRead(Type.STRING);
    }

    @Nonnull
    public PacketBuffer writeString(@Nonnull String string, int maxLength) {
        return processWrite(Type.STRING, string);
    }

    @Nonnull
    public PacketBuffer writeResourceLocation(@Nonnull ResourceLocation resourceLocationIn) {
        return processWrite(Type.RESOURCE_LOCATION, resourceLocationIn);
    }

    @Nonnull
    @Override
    public ResourceLocation readResourceLocation() {
        return processRead(Type.RESOURCE_LOCATION);
    }

    @Nonnull
    @Override
    public PacketBuffer writeLong(long val) {
        return processWrite(Type.LONG, val);
    }

    @Override
    public long readLong() {
        return processRead(Type.LONG);
    }

    @Nonnull
    @Override
    public PacketBuffer writeInt(int val) {
        return processWrite(Type.INT, val);
    }

    @Override
    public int readInt() {
        return processRead(Type.INT);
    }

    private <T> PacketBuffer processWrite(Type<T> t, T data) {
        log(t, data);
        t.write.accept(internal, data);
        return this;
    }

    private <T> T processRead(Type<T> t) {
        T result = t.read.apply(internal);
        log(t, result);
        return result;
    }

    private <T> void log(Type<T> t, T data) {
        log.writeVarInt(t.index);
        t.write.accept(log, data);
    }

    public static class Type<T> {
        public static final List<Type<?>> INSTANCES = new ArrayList<>();

        public static final Type<byte[]> BYTE_ARRAY = new Type<>(
                "byte[]", PacketBuffer::writeByteArray, PacketBuffer::readByteArray
        );
        public static final Type<int[]> VARINT_ARRAY = new Type<>(
                "varint[]", PacketBuffer::writeVarIntArray, PacketBuffer::readVarIntArray
        );
        public static final Type<long[]> LONG_ARRAY = new Type<>(
                "long[]", PacketBuffer::writeLongArray, buf -> buf.readLongArray(null)
        );
        public static final Type<BlockPos> BLOCK_POS = new Type<>(
                "BlockPos", PacketBuffer::writeBlockPos, PacketBuffer::readBlockPos
        );
        public static final Type<Integer> VARINT = new Type<>(
                "varint", PacketBuffer::writeVarInt, PacketBuffer::readVarInt
        );
        public static final Type<Long> VARLONG = new Type<>(
                "varlong", PacketBuffer::writeVarLong, PacketBuffer::readVarLong
        );
        public static final Type<Integer> INT = new Type<>(
                "int", PacketBuffer::writeInt, PacketBuffer::readInt
        );
        public static final Type<Long> LONG = new Type<>(
                "long", PacketBuffer::writeLong, PacketBuffer::readLong
        );
        public static final Type<UUID> UUID = new Type<>(
                "UUID", PacketBuffer::writeUniqueId, PacketBuffer::readUniqueId
        );
        public static final Type<String> STRING = new Type<>(
                "String", PacketBuffer::writeString, PacketBuffer::readString
        );
        public static final Type<ResourceLocation> RESOURCE_LOCATION = new Type<>(
                "ResourceLocation", PacketBuffer::writeResourceLocation, PacketBuffer::readResourceLocation
        );
        public static final Type<CompoundNBT> COMPOUND_TAG = new Type<>(
                "CompoundNBT", PacketBuffer::writeCompoundTag, PacketBuffer::readCompoundTag
        );

        private final int index;
        private final String name;
        private final BiConsumer<PacketBuffer, T> write;
        private final Function<PacketBuffer, T> read;

        private Type(String name, BiConsumer<PacketBuffer, T> write, Function<PacketBuffer, T> read) {
            this.name = name;
            this.index = INSTANCES.size();
            this.write = write;
            this.read = read;
            INSTANCES.add(this);
        }

        public void rerewrite(PacketBuffer source, PacketBuffer target, PacketBuffer targetAsInput, boolean log) {
            T temp = read.apply(source);
            if (log) {
                System.out.println("Rewriting " + temp + " (" + name + ")");
            }
            write.accept(target, temp);
            T reread = read.apply(targetAsInput);
            Preconditions.checkState(Objects.deepEquals(reread, temp));
        }
    }
}
