package com.allst.springboot.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * java8 开始扩展了可使用注解的地方。扩展后的这种注解称为类型注解（Type Annotation）。
 * 可以注解方法的返回类型，方法内this的类型，强制转换，数据级别，被继承的类，局部变量，以及throws等。
 * 还可以注解泛型，包括泛型类型参数边界和泛型参数
 * 定义类型注解, 因为它们允许工具对代码执行额外的检查，从而帮助避免错误。需要理解，javac本身一般不执行这些检查，所以需要使用单独的工具，
 * 不过这种工具可能需要使用编译器插件发挥作用。
 * 类型注解必须包含：ElementType.TYPE_USE
 *
 * @author Hutu
 * @since 2024-10-07 下午 03:41
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeAnno {
}
