package io.github.youseonghyeon.utils;

public class StringUtils {

    private StringUtils() {}

    public static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
