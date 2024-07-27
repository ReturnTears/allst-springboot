package com.allst.boot.word;

import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.Section;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.pages.FixedLayoutDocument;
import com.spire.doc.pages.FixedLayoutPage;

/**
 * @author Hutu
 * @since 2024-07-27 上午 09:48
 */
public class WordReadPage {
    public static void main(String[] args) {
        var document = new Document();
        document.loadFromFile(WordConstants.fileName.getPath());
        FixedLayoutDocument layoutDoc = new FixedLayoutDocument(document);
        FixedLayoutPage page = layoutDoc.getPages().get(0);
        Section section = page.getSection();
        Paragraph paragraphStart = page.getColumns().get(0).getLines().getFirst().getParagraph();
        int startIndex = 0;
        if (paragraphStart != null) {
            startIndex = section.getBody().getChildObjects().indexOf(paragraphStart);
        }

        Paragraph paragraphEnd = page.getColumns().get(0).getLines().getLast().getParagraph();

        int endIndex = 0;
        if (paragraphEnd != null) {
            endIndex = section.getBody().getChildObjects().indexOf(paragraphEnd);
        }

        Document newdoc = new Document();

        Section newSection = newdoc.addSection();

        section.cloneSectionPropertiesTo(newSection);

        for (int i = startIndex; i <= endIndex; i++) {
            newSection.getBody().getChildObjects().add(section.getBody().getChildObjects().get(i).deepClone());
        }

        newdoc.saveToFile(WordConstants.fileName_2.getPath(), FileFormat.Docx);

        newdoc.close();
        newdoc.dispose();

        document.close();
        document.dispose();
    }
}
