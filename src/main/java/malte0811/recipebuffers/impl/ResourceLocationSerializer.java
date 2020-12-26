package malte0811.recipebuffers.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import malte0811.recipebuffers.util.ListSerializer;
import malte0811.recipebuffers.util.RecurringData;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.BitSet;
import java.util.List;

/**
 * 1. Send namespaces using RecurringData
 * 2. Split paths on '/' and '_' and send the resulting parts using RecurringData. This is a good idea in practice since
 * many parts are quite common (reduces path bytes by 50% compared to simply writing the string). Separators are send
 * as a bitset, i.e. 1 bit per separator
 */
public class ResourceLocationSerializer {
    private final RecurringData<String> namespaces;
    private final RecurringData<String> pathParts;
    private final PacketBuffer buffer;
    int rlPathBytes = 0;

    public ResourceLocationSerializer(PacketBuffer buffer, boolean reading) {
        this.buffer = buffer;
        this.namespaces = RecurringData.createForString(buffer, reading);
        this.pathParts = RecurringData.createForString(buffer, reading);
    }

    public void write(ResourceLocation toWrite) {
        namespaces.write(toWrite.getNamespace());
        final int lastSize = buffer.writerIndex();
        new SplitPath(toWrite.getPath()).write(buffer, pathParts);
        rlPathBytes += buffer.writerIndex() - lastSize;
    }

    public ResourceLocation read() {
        final String namespace = namespaces.read();
        return new ResourceLocation(namespace, new SplitPath(buffer, pathParts).assemble());
    }

    private static class SplitPath {
        private final List<String> parts;
        private final BitSet splitBySlash;

        private SplitPath(String path) {
            ImmutableList.Builder<String> parts = ImmutableList.builder();
            BooleanList splitBySlash = new BooleanArrayList();
            StringBuilder currentPart = new StringBuilder();
            for (char c : path.toCharArray()) {
                // Only split at the first 8 possible positions: RLs with more than 8 splits should be (very) rare in
                // practice, and this approach allows sending split data simply as a byte
                if (splitBySlash.size() < 8 && (c == '_' || c == '/')) {
                    splitBySlash.add(c == '/');
                    parts.add(currentPart.toString());
                    currentPart.setLength(0);
                } else {
                    currentPart.append(c);
                }
            }
            parts.add(currentPart.toString());
            this.parts = parts.build();
            this.splitBySlash = new BitSet(splitBySlash.size());
            for (int i = 0; i < splitBySlash.size(); ++i) {
                this.splitBySlash.set(i, splitBySlash.getBoolean(i));
            }
        }

        private SplitPath(PacketBuffer buffer, RecurringData<String> parts) {
            this.splitBySlash = BitSet.valueOf(new byte[]{buffer.readByte()});
            this.parts = ListSerializer.readList(buffer, $ -> parts.read());
        }

        public void write(PacketBuffer buffer, RecurringData<String> parts) {
            byte[] splitBytes = this.splitBySlash.toByteArray();
            if (splitBytes.length > 0) {
                Preconditions.checkState(splitBytes.length == 1);
                buffer.writeByte(splitBytes[0]);
            } else {
                buffer.writeByte(0);
            }
            ListSerializer.writeList(buffer, this.parts, ($, s) -> parts.write(s));
        }

        public String assemble() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < this.parts.size() - 1; ++i) {
                result.append(this.parts.get(i));
                if (this.splitBySlash.get(i)) {
                    result.append('/');
                } else {
                    result.append('_');
                }
            }
            result.append(this.parts.get(this.parts.size() - 1));
            return result.toString();
        }
    }
}
