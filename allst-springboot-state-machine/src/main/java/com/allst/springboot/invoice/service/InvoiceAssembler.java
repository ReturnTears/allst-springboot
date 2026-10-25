package com.allst.springboot.invoice.service;

import com.allst.springboot.invoice.domain.InvoiceApply;
import com.allst.springboot.invoice.domain.InvoiceEventCatalog;
import com.allst.springboot.tradeorder.service.EventOption;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class InvoiceAssembler {

    public InvoiceView toView(InvoiceApply invoice, boolean replayed) {
        InvoiceView view = new InvoiceView();
        view.setInvoiceNo(invoice.getInvoiceNo());
        view.setTitle(invoice.getTitle());
        view.setAmount(invoice.getAmount());
        view.setStatus(invoice.getStatus());
        view.setStatusLabel(invoice.getStatus().getLabel());
        view.setRejectReason(invoice.getRejectReason());
        view.setReplayed(replayed);
        view.setAllowedEvents(InvoiceEventCatalog.allowed(invoice.getStatus()).stream()
                .map(event -> new EventOption(event.name(), event.getLabel()))
                .collect(Collectors.toList()));
        return view;
    }
}
