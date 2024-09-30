package com.allst.boot.stream;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Hutu
 * @since 2024-09-30 下午 10:23
 */
public class StreamCreated {
    public static void main(String[] args) throws IOException {
        createStreamFromFile();
    }

    /**
     * 使用of()方法创建流
     */
    private static void createStreamOf() throws IOException {
        String content = StreamApp.getContents();
        // 流的创建
        Stream<String> words = Stream.of(content.split("\n"));
        words.forEach(System.out::println);
        words.close();

        Stream<String> stream = Stream.of("aa", "bb", "cc");
        System.out.println(stream);
    }

    private static void createArrayStream() throws IOException {
        Stream<Object> empty = Stream.empty();
        String[] str = {"aa", "bb", "cc"};
        Stream<String> stream = Arrays.stream(str, 1, 3);
        stream.forEach(System.out::println);
    }

    /**
     * 创建无限流
     */
    private static void createStreamGenerate() {
        Stream<Integer> stream = Stream.generate(() -> 1);
        stream.limit(10).forEach(System.out::println);
    }

    private static void createStreamGenerate2() {
        Stream<Double> stream = Stream.generate(Math::random);
        stream.limit(10).forEach(System.out::println);
    }

    /**
     * 创建无限流, 迭代
     */
    private static void createStreamIterate() {
        Stream<BigInteger> stream = Stream.iterate(BigInteger.ZERO, item -> item.add(BigInteger.ONE));
        stream.limit(10).forEach(System.out::println);
    }

    /**
     * 创建有限流, 迭代， 需要添加一个谓语判断来指定迭代应在何时完成
     */
    private static void createStreamIterate2() {
        var limit = new BigInteger("100");
        Stream<BigInteger> stream = Stream.iterate(BigInteger.ZERO,
                n -> n.compareTo(limit) < 0, item -> item.add(BigInteger.ONE));
        stream.forEach(System.out::println);
    }

    /**
     * Stream.ofNullable 创建一个非常短的流
     */
    private static void createStreamOfNullable() {
        Stream<Object> objectStream = Stream.ofNullable(null);
        objectStream.forEach(System.out::println);
        objectStream.close();
        objectStream = Stream.ofNullable("Hello");
        objectStream.forEach(System.out::println);
    }

    /**
     * 注意：Java API中的许多方法都会产生流。例如，String类有一个lines方法，该方法生成一个由字符串中所有的行构成的流
     */
    private static void createStreamLines() {
        Stream<String> linestream = "Hello Stream".lines();
        linestream.forEach(System.out::println);
    }

    /**
     * Pattern类有一个通过正则表达式拆分CharSequence的splitAsStream方法
     */
    private static void createStreamSplitAsStream() throws IOException {
        String content = StreamApp.getContents();
        Stream<String> stream = Pattern.compile("\n").splitAsStream(content);
        stream.forEach(System.out::println);
    }

    /**
     * Scanner.tokens方法生成扫描器的标记流。从字符串中获取单词流
     */
    private static void createStreamTokens() throws IOException {
        String content = StreamApp.getContents();
        Stream<String> stream = new Scanner(content).tokens();
        stream.forEach(System.out::println);
    }

    /**
     * 静态方法Files.lines可以返回一个包含文件中所有行的Stream
     */
    private static void createStreamFromFile() throws IOException {
        String path = "E:\\Idea2020Projects\\allst-springboot\\allst-springboot-webflux";
        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            stream.forEach(System.out::println);
        }
    }
}
