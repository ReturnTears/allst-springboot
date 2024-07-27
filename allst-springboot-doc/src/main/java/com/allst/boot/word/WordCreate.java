package com.allst.boot.word;

import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.Section;
import com.spire.doc.documents.HorizontalAlignment;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.ParagraphStyle;

import java.awt.*;

/**
 * @author Hutu
 * @since 2024-07-27 上午 09:16
 */
public class WordCreate {
    public static void main(String[] args) {
        Document doc = new Document();
        Section section = doc.addSection();
        section.getPageSetup().getMargins().setAll(40f);
        Paragraph titleParagraph = section.addParagraph();
        titleParagraph.appendText("《星陨之城》");
        Paragraph bodyParagraph_1 = section.addParagraph();
        bodyParagraph_1.appendText("星陨之城曾是星际旅行者的圣地，一个充满希望和梦想的太空站。然而，随着一次灾难性的事件，太空站与外界失去了联系，成为了一个被遗忘的角落。传说，在太空站的核心，隐藏着一种名为星核的神秘力量，它能够操控星辰的轨迹，甚至改变命运。但星核的力量也是一把双刃剑，它不仅带来了希望，也带来了毁灭。玩家们被选中进入星陨之城，他们必须在这座废弃的太空站中寻找星核的碎片，解开其力量的秘密。但太空站并不是空无一人，异星生物在暗处潜伏，前工作人员的幽灵在走廊中游荡。玩家们必须在这场生死游戏中找到生存的希望，或是成为星陨之城的又一个传说。");

        Paragraph bodyParagraph_2 = section.addParagraph();
        bodyParagraph_2.appendText("真相：星核的力量并非自然形成，而是太空站的科学家们在进行一项禁忌实验时意外创造的。这项实验试图通过操控星辰的轨迹来改变人类的命运，但最终失控，导致了太空站的毁灭。");

        ParagraphStyle style1 = new ParagraphStyle(doc);
        style1.setName("titleStyle");
        style1.getCharacterFormat().setBold(true);;
        style1.getCharacterFormat().setTextColor(Color.BLUE);
        style1.getCharacterFormat().setFontName("Times New Roman");
        style1.getCharacterFormat().setFontSize(12f);
        doc.getStyles().add(style1);
        titleParagraph.applyStyle("titleStyle");

        ParagraphStyle style2 = new ParagraphStyle(doc);
        style2.setName("paraStyle");
        style2.getCharacterFormat().setFontName("Times New Roman");
        style2.getCharacterFormat().setFontSize(12);
        doc.getStyles().add(style2);
        bodyParagraph_1.applyStyle("paraStyle");
        bodyParagraph_2.applyStyle("paraStyle");

        titleParagraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        bodyParagraph_1.getFormat().setHorizontalAlignment(HorizontalAlignment.Justify);
        bodyParagraph_2.getFormat().setHorizontalAlignment(HorizontalAlignment.Justify);

        bodyParagraph_1.getFormat().setFirstLineIndent(30) ;
        bodyParagraph_2.getFormat().setFirstLineIndent(30);

        titleParagraph.getFormat().setAfterSpacing(10);
        bodyParagraph_1.getFormat().setAfterSpacing(10);

        doc.saveToFile(WordConstants.fileName.getPath(), FileFormat.Docx_2013);
        doc.close();
    }
}
