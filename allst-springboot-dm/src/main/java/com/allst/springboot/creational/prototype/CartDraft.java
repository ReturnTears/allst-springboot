package com.allst.springboot.creational.prototype;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring 的 prototype 作用域：每次取都是新实例，用来放「一次请求内的可变草稿」。
 * <Pre>
 * 坑：把 prototype bean 直接 @Autowired 进单例 bean，注入只发生一次，
 * 那个 prototype 实际上被永久复用。正确取法见 CartDraftProvider。
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CartDraft {

    private final List<String> items = new ArrayList<>();

    public void add(String sku) {
        items.add(sku);
    }

    public List<String> items() {
        return Collections.unmodifiableList(items);
    }
}
