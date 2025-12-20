package com.allst.boot.word;

/**
 * @author Hutu
 * @since 2024-07-27 上午 09:37
 */
@lombok.Getter
public enum WordConstants {
    fileName("E:\\TestData\\other\\WordDocument.docx"),

    fileName_2("E:\\TestData\\other\\WordDocument2.docx"),
    fileName_3("E:\\TestData\\other\\Docx4jWord1.docx");

    private final String path;

    WordConstants(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
