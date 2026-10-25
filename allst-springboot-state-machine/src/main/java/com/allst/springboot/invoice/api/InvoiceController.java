package com.allst.springboot.invoice.api;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.invoice.domain.InvoiceEventCommand;
import com.allst.springboot.invoice.service.InvoiceAppService;
import com.allst.springboot.invoice.service.InvoiceView;
import com.allst.springboot.tradeorder.service.TransitionLogView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceAppService appService;

    public InvoiceController(InvoiceAppService appService) {
        this.appService = appService;
    }

    @PostMapping
    public Result<InvoiceView> create(@Valid @RequestBody CreateInvoiceRequest request) {
        return Result.ok(appService.create(request.getTitle(), request.getAmount()));
    }

    @GetMapping("/{invoiceNo}")
    public Result<InvoiceView> get(@PathVariable String invoiceNo) {
        return Result.ok(appService.get(invoiceNo));
    }

    @GetMapping("/{invoiceNo}/logs")
    public Result<List<TransitionLogView>> logs(@PathVariable String invoiceNo) {
        return Result.ok(appService.logs(invoiceNo));
    }

    @GetMapping("/meta/plantuml")
    public Result<String> uml() {
        return Result.ok(appService.plantUml());
    }

    @PostMapping("/{invoiceNo}/events")
    public Result<InvoiceView> fire(@PathVariable String invoiceNo, @Valid @RequestBody InvoiceEventRequest request) {
        InvoiceEventCommand command = new InvoiceEventCommand();
        command.setEvent(request.getEvent());
        command.setOperator(request.getOperator());
        command.setRole(request.getRole());
        command.setEventId(request.getEventId());
        command.setRemark(request.getRemark());
        return Result.ok(appService.fire(invoiceNo, command));
    }
}
