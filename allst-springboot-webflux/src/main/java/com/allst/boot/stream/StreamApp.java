package com.allst.boot.stream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Hutu
 * @since 2024-09-30 下午 09:59
 */
public class StreamApp {
    public static void main(String[] args) throws IOException {
        countEn();
    }

    private static void countCn() throws IOException {
        String contents = Files.readString(Path.of("E:\\Idea2020Projects\\allst-springboot\\allst-springboot-webflux\\src\\main\\resources\\poem.txt"), StandardCharsets.UTF_8);
        long count = 0;
        count += countChineseCharacters(contents);
        System.out.println(count);
    }

    private static int countChineseCharacters(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // 检查字符是否在指定的Unicode范围内
            if (c >= '一' && c <= '龥') {
                count++;
            }
        }
        return count;
    }

    private static void countEn() throws IOException {
        String contents = Files.readString(Path.of("E:\\Idea2020Projects\\allst-springboot\\allst-springboot-webflux\\src\\main\\resources\\words.txt"), StandardCharsets.UTF_8);
        List<String> words = List.of(contents.split(" "));
        long count = words.stream().filter(word -> word.matches("[a-zA-Z]+")).count();
        System.out.println(count);
    }

    protected static String getContents() throws IOException {
        return Files.readString(Path.of("E:\\Idea2020Projects\\allst-springboot\\allst-springboot-webflux\\src\\main\\resources\\words.txt"), StandardCharsets.UTF_8);
    }
}
