package malte0811.recipebuffers.util;

import com.mojang.datafixers.types.Func;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

public abstract class RecurringData<T> {
    protected final List<T> known = new ArrayList<>();
    protected final PacketBuffer io;

    protected RecurringData(PacketBuffer io) {
        this.io = io;
    }

    public static <T> RecurringData<T> reader(PacketBuffer buffer, Function<PacketBuffer, T> read) {
        return new Reader<>(buffer, read);
    }

    public static <T> RecurringData<T> writer(
            PacketBuffer buffer, BiPredicate<T, T> equal, BiConsumer<PacketBuffer, T> write
    ) {
        return new Writer<>(buffer, equal, write);
    }

    public static <T> RecurringData<T> create(
            PacketBuffer buffer,
            Function<PacketBuffer, T> read,
            BiPredicate<T, T> equal,
            BiConsumer<PacketBuffer, T> write,
            boolean reader
    ) {
        if (reader) {
            return reader(buffer, read);
        } else {
            return writer(buffer, equal, write);
        }
    }

    public static <T> RecurringData<List<T>> createForList(
            PacketBuffer buffer,
            Function<PacketBuffer, T> read,
            BiPredicate<T, T> equal,
            BiConsumer<PacketBuffer, T> write,
            boolean reader
    ) {
        return create(
                buffer,
                pb -> ListSerializer.readList(pb, read),
                (l1, l2) -> {
                    if (l1.size() != l2.size()) {
                        return false;
                    }
                    for (int i = 0; i < l1.size(); ++i) {
                        if (!equal.test(l1.get(i), l2.get(i))) {
                            return false;
                        }
                    }
                    return true;
                },
                (pb, l) -> ListSerializer.writeList(pb, l, write),
                reader
        );
    }

    public abstract void write(T value);

    public abstract T read();

    public abstract <T2> RecurringData<T2> xmap(Function<T, T2> to, Function<T2, T> from);

    private static class Reader<T> extends RecurringData<T> {
        private final Function<PacketBuffer, T> read;

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
                return known.get(id);
            }
        }

        @Override
        public <T2> RecurringData<T2> xmap(Function<T, T2> to, Function<T2, T> from) {
            return new Reader<>(io, read.andThen(to));
        }
    }

    private static class Writer<T> extends RecurringData<T> {
        protected final BiPredicate<T, T> equal;
        protected final BiConsumer<PacketBuffer, T> write;

        protected Writer(
                PacketBuffer io, BiPredicate<T, T> equal, BiConsumer<PacketBuffer, T> write
        ) {
            super(io);
            this.equal = equal;
            this.write = write;
        }

        @Override
        public void write(T value) {
            int id = -1;
            for (int i = 0; i < known.size(); ++i) {
                if (equal.test(value, known.get(i))) {
                    id = i;
                    break;
                }
            }
            io.writeVarInt(id + 1);
            if (id < 0) {
                write.accept(io, value);
                known.add(value);
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
                    (x, y) -> equal.test(from.apply(x), from.apply(y)),
                    (pb, t2) -> write.accept(pb, from.apply(t2))
            );
        }
    }
}
