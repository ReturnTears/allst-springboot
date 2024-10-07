package com.allst.springboot.entity;

import com.allst.springboot.anno.*;

/**
 * use an annotation on a type parameter
 *
 * @author Hutu
 * @since 2024-10-07 下午 04:53
 */
public class TypeAnnoDemo<@What(description = "Generic data type") T> {
    /**
     * use a type annotation on a constructor
     */
    public @Unique TypeAnnoDemo() {

    }

    /**
     * annotation the type (in this case String), not the field
     */
    @TypeAnno
    String str;

    /**
     * this annotates the field test
     */
    @EmptyOk
    String test;

    /**
     * use a type annotation to annotate this (the receiver)
     */
    public int f(@TypeAnno TypeAnnoDemo<T> this, int y) {
        return y + 10;
    }

    /**
     * annotate the return type
     */
    public @TypeAnno Integer f2(int j, int k) {
        return j + k;
    }

    /**
     * annotate the method declaration
     */
    public @Recommended Integer f3(String str) {
        return str.length() / 2;
    }

    /**
     *  use a type annotation with a throws clause
     */
    public void f4() throws @TypeAnno NullPointerException {
    }

    /**
     * annotate array levels
     */
    String @MaxLen(10)[] @NotZeroLen[] w;

    /**
     * annotate the array element type
     */
    @TypeAnno
    Integer[] vec;

    public static void myMeth(int i) {
        TypeAnnoDemo<@TypeAnno Integer> ob = new TypeAnnoDemo<@TypeAnno Integer>();
        System.out.println("ob.f(100) = " + ob.f(100));

        @Unique TypeAnnoDemo<Integer> ob2 = new @Unique TypeAnnoDemo<>();
        System.out.println("ob2.f2(10, 20) = " + ob2.f2(10, 20));

        Object x = i;
        Integer y = (@TypeAnno Integer) x;
        System.out.println("y = " + y);
    }

    public static void main(String[] args) {
        myMeth(10);
    }

    class SomeClass extends @TypeAnno TypeAnnoDemo<Boolean> {

    }
}
