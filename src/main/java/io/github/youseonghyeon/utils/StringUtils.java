package io.github.youseonghyeon.utils;

public class StringUtils {

    public static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
