package com.allst.boot.word;

/**
 * @author Hutu
 * @since 2024-07-27 上午 09:37
 */
@lombok.Getter
public enum WordConstants {
    fileName("D:\\TestData\\other\\WordDocument.docx"),

    fileName_2("D:\\TestData\\other\\WordDocument2.docx"),
    fileName_3("D:\\TestData\\other\\Docx4jWord1.docx");

    private final String path;

    WordConstants(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
