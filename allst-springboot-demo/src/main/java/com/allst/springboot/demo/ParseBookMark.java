package com.allst.springboot.demo;

import cn.hutool.core.util.StrUtil;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hutu
 * @since 2025-07-26 下午 10:48
 */
public class ParseBookMark {
    public static void main(String[] args) {
        File file = new File("C:\\Users\\June\\Desktop\\bookmarks.html");
        try (FileInputStream fis = new FileInputStream(file)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                if (StrUtil.containsAll(line, "HREF", "</A>")) {
                    //System.out.println(line);
                    String regex = ">([^<]+)</A>";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(line);
                    String regex2 = "HREF=\"([^\"]+)\"";
                    Pattern pattern2 = Pattern.compile(regex2);
                    Matcher matcher2 = pattern2.matcher(line);
                    while (matcher.find() && matcher2.find()) {
                        //System.out.println("提取到的内容: " + matcher.group(1));
                        String decodedStr = decodeMixedString(matcher2.group(1));
                        System.out.println(matcher.group(1) + ":" + decodedStr);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decodeMixedString(String input) {
        try {
            // 分割字符串，处理每个以%开头的部分
            StringBuilder result = new StringBuilder();
            String[] parts = input.split("(?=%|\\z)"); // 按%分割

            for (String part : parts) {
                if (part.startsWith("%")) {
                    // 解码URL编码部分
                    result.append(URLDecoder.decode(part, StandardCharsets.UTF_8));
                } else {
                    // 直接添加普通文本
                    result.append(part);
                }
            }
            return result.toString();
        } catch (Exception e) {
            return input; // 出错时返回原始字符串
        }
    }
}
