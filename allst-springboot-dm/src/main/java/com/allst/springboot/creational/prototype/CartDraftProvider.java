package com.allst.springboot.creational.prototype;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 单例服务里需要原型对象时，用 ObjectProvider（或 @Lookup / scoped proxy）每次现取。
 */
@Component
public class CartDraftProvider {

    private final ObjectProvider<CartDraft> drafts;

    public CartDraftProvider(ObjectProvider<CartDraft> drafts) {
        this.drafts = drafts;
    }

    public CartDraft newDraft() {
        return drafts.getObject();
    }
}
