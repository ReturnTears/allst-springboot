package com.allst.springboot.invoice.domain;

import java.util.List;

public final class InvoiceEventCatalog {

    private InvoiceEventCatalog() {
    }

    public static List<InvoiceEvent> allowed(InvoiceState state) {
        switch (state) {
            case DRAFT:
                return List.of(InvoiceEvent.SUBMIT, InvoiceEvent.VOID);
            case PENDING_REVIEW:
                return List.of(InvoiceEvent.APPROVE, InvoiceEvent.REJECT);
            case REJECTED:
                return List.of(InvoiceEvent.RESUBMIT, InvoiceEvent.VOID);
            case ISSUED:
                return List.of(InvoiceEvent.RED_FLUSH);
            default:
                return List.of();
        }
    }
}
