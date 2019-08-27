package me.mdbell.util;

import java.nio.ByteBuffer;

public interface IPatternMatcher {

    int match(ByteBuffer buffer);
}
