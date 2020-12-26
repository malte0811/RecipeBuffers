package malte0811.recipebuffers.impl;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import malte0811.recipebuffers.RecipeBuffers;
import malte0811.recipebuffers.util.StateStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.HexDumper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class ErrorLogger {
    private static final int BYTES_EITHER_WAY = 200;

    public static void logReadError(Throwable error, PacketBuffer currentBufferState, int startIndex) {
        printSanitizedExceptionAndStateStack(error);

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
        printSanitizedExceptionAndStateStack(error);

        final int currentIndex = currentBufferState.writerIndex();
        final int outputIndex = Math.max(currentIndex - BYTES_EITHER_WAY, startIndex);
        final byte[] bytesToWrite = new byte[currentIndex - outputIndex];
        currentBufferState.getBytes(outputIndex, bytesToWrite);
        RecipeBuffers.LOGGER.info(
                "Last bytes written:\n{}",
                hexdump(bytesToWrite, 0, bytesToWrite.length)
        );
    }

    private static void printSanitizedExceptionAndStateStack(Throwable error) {
        // Convert exception into a "partial hexdump": When a RL read fails, the log will generally contain many
        // unprintable characters, which can cause "interesting" behavior in some text editors.
        final CharList specialAllowed = new CharArrayList(new char[]{'\n', '\r', '\t'});
        ByteArrayOutputStream rawException = new ByteArrayOutputStream();
        error.printStackTrace(new PrintStream(rawException));
        StringBuilder toWrite = new StringBuilder(rawException.size());
        int numUnprintable = 0;
        for (byte b : rawException.toByteArray()) {
            final int value = b & 0xff;
            if ((value < 0x20 || value == 0xff) && !specialAllowed.contains((char) value)) {
                //Not printable
                ++numUnprintable;
                toWrite.append('<').append(Integer.toHexString(value)).append('>');
            } else {
                toWrite.append((char) value);
            }
        }
        RecipeBuffers.LOGGER.info("Sanitized exception (replaced {} characters):\n{}", numUnprintable, toWrite);
        RecipeBuffers.LOGGER.info("State stack at error:\n{}", StateStack.formatAndClear());
    }

    private static String hexdump(byte[] bytes, int start, int end) {
        byte[] subBytes = Arrays.copyOfRange(bytes, start, end);
        return HexDumper.dump(subBytes);
    }
}
