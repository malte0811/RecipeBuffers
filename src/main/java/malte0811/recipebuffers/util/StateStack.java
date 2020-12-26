package malte0811.recipebuffers.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a threadlocal stack of "logical" state that can be used to provide additional information for crashes
 */
public class StateStack {
    private final static ThreadLocal<List<String>> CURRENT_STACK = ThreadLocal.withInitial(ArrayList::new);

    public static Entry push(String state) {
        final List<String> threadStack = CURRENT_STACK.get();
        threadStack.add(state);
        return new StateStack.Entry(state, threadStack.size() - 1);
    }

    public static String formatAndClear() {
        final char corner = '\u2514';
        final StringBuilder result = new StringBuilder();
        final List<String> threadStack = CURRENT_STACK.get();
        String spacePrefix = "";
        for (int i = 0; i < threadStack.size(); i++) {
            String element = threadStack.get(i);
            if (i > 0) {
                result.append(spacePrefix).append(corner).append(' ');
            }
            result.append(element).append('\n');
            if (i > 0) {
                spacePrefix += ' ';
            }
        }
        threadStack.clear();
        return result.toString();
    }

    public static void assertEmpty() {
        if (!CURRENT_STACK.get().isEmpty()) {
            throw new IllegalStateException("Expected empty stack (see log for current state stack)");
        }
    }

    // RAII doesn't really exist in Java, so this will have to do
    public static class Entry {
        private final String name;
        private final int expectedIndexAtPop;
        private boolean popped = false;

        public Entry(String name, int expectedIndexAtPop) {
            this.name = name;
            this.expectedIndexAtPop = expectedIndexAtPop;
        }

        public void pop() {
            Preconditions.checkState(!popped);
            final List<String> threadStack = CURRENT_STACK.get();
            final int indexToRemove = threadStack.size() - 1;
            Preconditions.checkState(
                    indexToRemove == expectedIndexAtPop,
                    "Expected to pop \"" + name + "\" at index " + expectedIndexAtPop + ", but index is " + indexToRemove
            );
            final String elementToRemove = threadStack.get(indexToRemove);
            Preconditions.checkState(
                    elementToRemove.equals(name),
                    "Tried to pop \"" + name + "\", but last element is \"" + elementToRemove + "\""
            );
            threadStack.remove(indexToRemove);
            popped = true;
        }
    }
}
