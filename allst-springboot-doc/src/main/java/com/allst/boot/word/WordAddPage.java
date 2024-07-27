package com.allst.boot.word;

import com.spire.doc.Body;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.documents.BreakType;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.ParagraphStyle;

/**
 * @author Hutu
 * @since 2024-07-27 上午 09:36
 */
public class WordAddPage {
    public static void main(String[] args) {
        var document = new Document();
        document.loadFromFile(WordConstants.fileName.getPath());
        Body body = document.getLastSection().getBody();
        body.getLastParagraph().appendBreak(BreakType.Page_Break);
        ParagraphStyle paragraphStyle = new ParagraphStyle(document);
        paragraphStyle.setName("CustomParagraphStyle1");
        paragraphStyle.getParagraphFormat().setLineSpacing(12);
        paragraphStyle.getParagraphFormat().setAfterSpacing(8);
        paragraphStyle.getCharacterFormat().setFontName("Microsoft YaHei");
        paragraphStyle.getCharacterFormat().setFontSize(12);
        document.getStyles().add(paragraphStyle);

        Paragraph paragraph = new Paragraph(document);
        paragraph.appendText("背景故事：星陨之城的传说从未停歇，玩家们在首次探险后，发现了隐藏在星核深处的密室。这里藏有遗失的星核碎片，它们是连接过去与未来的钥匙。玩家们被召唤回到星陨之城，但这一次，他们不仅要面对异星生物的威胁，还要与时间赛跑，探索一个古老文明的遗迹，寻找救赎之路。");

        paragraph.applyStyle(paragraphStyle.getName());
        body.getChildObjects().add(paragraph);

        paragraph = new Paragraph(document);
        paragraph.appendText("真相：星核的力量并非源自自然，而是那个古老文明的科技结晶。他们试图通过星核来实现时间旅行，却因无法控制其力量而导致文明的毁灭。星陨之城成为了他们最后的遗产，也是对未来的警示。");

        paragraph.applyStyle(paragraphStyle.getName());
        body.getChildObjects().add(paragraph);
        document.saveToFile(WordConstants.fileName.getPath(), FileFormat.Docx);
        document.close();

        document.dispose();
    }
}
