package com.allst.springboot.service.impl;

import com.allst.springboot.anno.Hint;
import com.allst.springboot.service.HintService;

/**
 * @author Hutu
 * @since 2024-10-08 下午 11:24
 */
public class HintServiceImpl {
    public static void main(String[] args) {
        Hint hint = HintService.class.getAnnotation(Hint.class);
        System.out.println(hint);
        Hint[] hints = HintService.class.getAnnotationsByType(Hint.class);
        for (Hint hint1 : hints) {
            System.out.println(hint1.value());
        }
    }
}
