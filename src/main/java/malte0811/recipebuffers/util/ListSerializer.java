package malte0811.recipebuffers.util;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ListSerializer {

    public static <T> void writeList(PacketBuffer out, Collection<T> values, BiConsumer<PacketBuffer, T> write) {
        out.writeVarInt(values.size());
        for (T val : values) {
            write.accept(out, val);
        }
    }

    public static <T> List<T> readList(PacketBuffer in, Function<PacketBuffer, T> read) {
        final int numValues = in.readVarInt();
        List<T> values = new ArrayList<>(numValues);
        for (int i = 0; i < numValues; ++i) {
            values.add(read.apply(in));
        }
        return values;
    }
}
