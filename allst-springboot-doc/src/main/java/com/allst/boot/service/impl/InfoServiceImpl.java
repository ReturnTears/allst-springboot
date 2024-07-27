package com.allst.boot.service.impl;

import com.allst.boot.model.InfoBo;
import com.allst.boot.service.InfoService;
import com.allst.boot.word.InfoConstants;
import com.google.common.collect.Lists;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * @author Hutu
 * @since 2024-07-27 下午 05:46
 */
@Service
public class InfoServiceImpl implements InfoService {
    @Override
    public XWPFDocument doc2Word() {
        XWPFDocument doc = new XWPFDocument();
        createTitle(doc, "《星陨之城》");

        createChapterH1(doc, "主题内核：生存");
        createChapterH2(doc, "场景名称：星陨之城");
        createParagraph(doc, "场景描述：一个位于遥远星系边缘的废弃太空站，传说中是星际旅行者的最后归宿。");
        createChapterH2(doc, "关键元素：星核，一种能够操控星辰轨迹的神秘力量源泉。");
        createParagraph(doc, "游戏规则：玩家必须在星陨之城中寻找星核碎片，解开其力量，同时抵御来自异星生物的侵袭。");

        createChapterH1(doc, "剧情发展：玩家在探索星陨之城的过程中，逐渐揭开太空站废弃的真相和星核的秘密。");
        createChapterH2(doc, "NPC角色：太空站的前工作人员，他们或许知道星核的秘密，但也可能因长时间的孤独而变得疯狂。");
        createParagraph(doc, "死亡方式：异星生物的攻击、太空站的陷阱、解谜失败导致的爆炸。");

        List<InfoBo> userList = getInfoList();
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFTable table = paragraph.getDocument().createTable(userList.size(), 3);
        table.setWidth(500);
        table.setCellMargins(20, 20, 20, 20);

        CTTblPr tablePr = table.getCTTbl().addNewTblPr();
        CTTblWidth width = tablePr.addNewTblW();
        width.setW(BigInteger.valueOf(8000));

        for(int i = 0; i< userList.size(); i++) {
            List<XWPFTableCell> tableCells = table.getRow(i).getTableCells();
            tableCells.get(0).setText(userList.get(i).getId()+"");
            tableCells.get(1).setText(userList.get(i).getName());
            tableCells.get(2).setText(userList.get(i).getDesc());
        }

        createChapterH2(doc, "图片导出示例");
        createParagraph(doc, "以导出图片为例");

        // 图片
        InputStream stream = null;
        try {
            XWPFParagraph paragraph2 = doc.createParagraph();
            Resource resource = new ClassPathResource("qrcode.png");
            stream = new FileInputStream(resource.getFile());
            XWPFRun run = paragraph2.createRun();
            run.addPicture(stream, Document.PICTURE_TYPE_PNG, "Generated", Units.toEMU(256), Units.toEMU(256));
        } catch (IOException | InvalidFormatException e) {
        }

        return doc;
    }

    @Override
    public List<InfoBo> getInfoList() {
        List<InfoBo> list = Lists.newArrayList();
        List<String> nameList = InfoConstants.nameList;
        List<String> descList = InfoConstants.descList;
        for (int i = 0; i < nameList.size(); i++) {
            InfoBo bo = new InfoBo();
            bo.setId(i);
            bo.setName(nameList.get(i));
            bo.setDesc(descList.get(i));
            list.add(bo);
        }
        return list;
    }
}
