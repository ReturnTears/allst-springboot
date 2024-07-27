package com.allst.boot.word;

/**
 * @author Hutu
 * @since 2024-07-27 上午 09:37
 */
@lombok.Getter
public enum WordConstants {
    fileName("E:\\TestData\\other\\WordDocument.docx"),

    fileName_2("E:\\TestData\\other\\WordDocument2.docx");

    private final String path;

    WordConstants(String path) {
        this.path = path;
    }
}
