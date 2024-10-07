package com.allst.springboot.service.impl;

import com.allst.springboot.anno.TypeAnno;
import org.springframework.stereotype.Component;

/**
 * @author Hutu
 * @since 2024-10-07 下午 03:48
 */
@Component
public class TypeAnnoService {
    public void myMethod(@TypeAnno TypeAnnoService this, int i, int j) {
        System.out.println("this = " + this);
        System.out.println("i = " + i);
    }
}
