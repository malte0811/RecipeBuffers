package malte0811.recipebuffers.util;

import com.mojang.datafixers.types.Func;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Handles sending data that recurs commonly. If a value has been seen before it does not send the full value, but
 * instead sends the index in the list of unique values.
 */
public abstract class RecurringData<T> {
    protected final PacketBuffer io;
    protected int hits = 0;

    protected RecurringData(PacketBuffer io) {
        this.io = io;
    }

    public static <T> RecurringData<T> reader(PacketBuffer buffer, Function<PacketBuffer, T> read) {
        return new Reader<>(buffer, read);
    }

    public static <T> RecurringData<T> writer(
            PacketBuffer buffer, BiConsumer<PacketBuffer, T> write
    ) {
        return new Writer<>(buffer, write);
    }

    public static <T> RecurringData<T> create(
            PacketBuffer buffer,
            Function<PacketBuffer, T> read,
            BiConsumer<PacketBuffer, T> write,
            boolean reader
    ) {
        if (reader) {
            return reader(buffer, read);
        } else {
            return writer(buffer, write);
        }
    }

    public static <T> RecurringData<List<T>> createForList(
            PacketBuffer buffer,
            Function<PacketBuffer, T> read,
            BiConsumer<PacketBuffer, T> write,
            boolean reader
    ) {
        return create(
                buffer,
                pb -> ListSerializer.readList(pb, read),
                (pb, l) -> ListSerializer.writeList(pb, l, write),
                reader
        );
    }

    public abstract void write(T value);

    public abstract T read();

    public abstract <T2> RecurringData<T2> xmap(Function<T, T2> to, Function<T2, T> from);

    public abstract int size();

    public int hits() {
        return hits;
    }

    private static class Reader<T> extends RecurringData<T> {
        private final Function<PacketBuffer, T> read;
        protected final List<T> known = new ArrayList<>();

        protected Reader(PacketBuffer io, Function<PacketBuffer, T> read) {
            super(io);
            this.read = read;
        }

        @Override
        public void write(T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T read() {
            int id = io.readVarInt() - 1;
            if (id < 0) {
                T result = read.apply(io);
                known.add(result);
                return result;
            } else {
                ++hits;
                return known.get(id);
            }
        }

        @Override
        public <T2> RecurringData<T2> xmap(Function<T, T2> to, Function<T2, T> from) {
            return new Reader<>(io, read.andThen(to));
        }

        @Override
        public int size() {
            return known.size();
        }
    }

    private static class Writer<T> extends RecurringData<T> {
        protected final BiConsumer<PacketBuffer, T> write;
        protected final Object2IntMap<T> known = new Object2IntOpenHashMap<>();

        protected Writer(PacketBuffer io, BiConsumer<PacketBuffer, T> write) {
            super(io);
            this.write = write;
        }

        @Override
        public void write(T value) {
            int id = known.getOrDefault(value, -1);
            // Add one because -1 takes 5 bytes as a varint
            io.writeVarInt(id + 1);
            if (id < 0) {
                write.accept(io, value);
                known.put(value, known.size());
            } else {
                ++hits;
            }
        }

        @Override
        public T read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T2> RecurringData<T2> xmap(Function<T, T2> to, Function<T2, T> from) {
            return new Writer<>(
                    io,
                    (pb, t2) -> write.accept(pb, from.apply(t2))
            );
        }

        @Override
        public int size() {
            return known.size();
        }
    }
}
