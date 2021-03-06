package malte0811.recipebuffers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import malte0811.recipebuffers.impl.OptimizedPacketBuffer;
import malte0811.recipebuffers.util.LoggingPacketBuffer;
import net.minecraft.network.PacketBuffer;

import java.io.FileInputStream;
import java.io.IOException;

public class Reparse {
    public static void main(String[] args) throws IOException {
        FileInputStream input = new FileInputStream("run/parsable.dmp");
        ByteBuf inputBuffer = Unpooled.buffer();
        int next;
        while ((next = input.read()) != -1) {
            inputBuffer.writeByte(next);
        }
        inputBuffer = inputBuffer.asReadOnly();
        for (int i = 0; i < 10; ++i) {
            reparse(inputBuffer.duplicate());
        }
    }

    private static void reparse(ByteBuf inputBuffer) {
        PacketBuffer inputPB = new PacketBuffer(inputBuffer);
        ByteBuf outputBuffer = Unpooled.buffer();
        PacketBuffer outputPB = new OptimizedPacketBuffer(outputBuffer, false);//PacketBuffer(outputBuffer);//
        PacketBuffer outputAsInput = new OptimizedPacketBuffer(outputBuffer, true);
        final long reparseStart = System.currentTimeMillis();
        int idBytes = 0;
        while (inputPB.readerIndex() != inputPB.writerIndex()) {
            final int id = inputPB.readVarInt();
            idBytes += PacketBuffer.getVarIntSize(id);
            LoggingPacketBuffer.Type.INSTANCES.get(id).rerewrite(inputPB, outputPB, outputAsInput, false);
        }
        System.out.printf("Plain size: %,d, opt size: %,d\n", inputPB.writerIndex() - idBytes, outputPB.writerIndex());
        System.out.println("Time for reparse: " + (System.currentTimeMillis() - reparseStart) + " ms");
    }
}
