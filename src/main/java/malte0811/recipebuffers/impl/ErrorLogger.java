package malte0811.recipebuffers.impl;

import malte0811.recipebuffers.RecipeBuffers;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.HexDumper;

import java.util.Arrays;

public class ErrorLogger {
    private static final int BYTES_EITHER_WAY = 200;

    public static void logReadError(Throwable error, PacketBuffer currentBufferState, int startIndex) {
        error.printStackTrace();
        final int currentIndex = currentBufferState.readerIndex();
        final int outputStartIndex = Math.max(startIndex, currentIndex - BYTES_EITHER_WAY);
        final int outputLength = (currentIndex - outputStartIndex) + Math.min(
                currentBufferState.readableBytes(),
                BYTES_EITHER_WAY
        );
        final byte[] bytesToWrite = new byte[outputLength];
        currentBufferState.getBytes(outputStartIndex, bytesToWrite);
        RecipeBuffers.LOGGER.info(
                "Bytes before current position:\n{}",
                hexdump(bytesToWrite, 0, currentIndex - outputStartIndex)
        );
        RecipeBuffers.LOGGER.info(
                "Bytes after current position:\n{}",
                hexdump(bytesToWrite, currentIndex - outputStartIndex, bytesToWrite.length)
        );
    }

    public static void logWriteError(Throwable error, PacketBuffer currentBufferState, int startIndex) {
        error.printStackTrace();
        final int currentIndex = currentBufferState.writerIndex();
        final int outputIndex = Math.max(currentIndex - BYTES_EITHER_WAY, startIndex);
        final byte[] bytesToWrite = new byte[currentIndex - outputIndex];
        currentBufferState.getBytes(outputIndex, bytesToWrite);
        RecipeBuffers.LOGGER.info(
                "Last bytes written:\n{}",
                hexdump(bytesToWrite, 0, bytesToWrite.length)
        );
    }

    private static String hexdump(byte[] bytes, int start, int end) {
        byte[] subBytes = Arrays.copyOfRange(bytes, start, end);
        return HexDumper.dump(subBytes);
    }
}
