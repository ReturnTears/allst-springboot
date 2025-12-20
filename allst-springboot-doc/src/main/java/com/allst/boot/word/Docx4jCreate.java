package com.allst.boot.word;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

import java.io.File;

/**
 * @author Hutu
 * @since 2025-03-05 下午 09:16
 */
public class Docx4jCreate {
    public static void main(String[] args) throws Docx4JException {
        WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
        MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();
        mainDocumentPart.addStyledParagraphOfText("Title", "Hello World!");
        mainDocumentPart.addParagraphOfText("Welcome To Docx4j");
        File exportFile = new File("Docx4jWord1.docx");
        wordPackage.save(exportFile);
    }
}
