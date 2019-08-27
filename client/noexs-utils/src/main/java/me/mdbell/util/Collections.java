package me.mdbell.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Collections {

    public static Iterator<Character> iterator(final String string) {
        if (string == null)
            throw new NullPointerException();

        return new Iterator<Character>() {
            private int index = 0;

            public boolean hasNext() {
                return index < string.length();
            }

            public Character next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return string.charAt(index++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
