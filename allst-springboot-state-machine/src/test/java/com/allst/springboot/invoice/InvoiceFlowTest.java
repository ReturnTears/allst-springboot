package com.allst.springboot.invoice;

import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.invoice.domain.InvoiceEvent;
import com.allst.springboot.invoice.domain.InvoiceEventCommand;
import com.allst.springboot.invoice.domain.InvoiceState;
import com.allst.springboot.invoice.service.InvoiceAppService;
import com.allst.springboot.invoice.service.InvoiceView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceFlowTest {

    @Autowired
    private InvoiceAppService appService;

    @Test
    @DisplayName("小额发票由普通角色审核开具，再红冲")
    void smallAmountThenRedFlush() {
        InvoiceView created = appService.create("办公用品", new BigDecimal("99.00"));
        InvoiceView pending = fire(created.getInvoiceNo(), InvoiceEvent.SUBMIT, "submit-1", "CLERK", null);
        assertThat(pending.getStatus()).isEqualTo(InvoiceState.PENDING_REVIEW);
        InvoiceView issued = fire(created.getInvoiceNo(), InvoiceEvent.APPROVE, "approve-1", "CLERK", null);
        assertThat(issued.getStatus()).isEqualTo(InvoiceState.ISSUED);
        InvoiceView voided = fire(created.getInvoiceNo(), InvoiceEvent.RED_FLUSH, "flush-1", "CLERK", "开错抬头");
        assertThat(voided.getStatus()).isEqualTo(InvoiceState.VOIDED);
        assertThat(appService.plantUml()).contains("DRAFT");
    }

    @Test
    @DisplayName("大额发票普通角色不能审核，财务角色可以")
    void largeAmountNeedsFinanceRole() {
        InvoiceView created = appService.create("设备款", new BigDecimal("10000"));
        fire(created.getInvoiceNo(), InvoiceEvent.SUBMIT, "submit-2", "CLERK", null);
        assertThatThrownBy(() -> fire(created.getInvoiceNo(), InvoiceEvent.APPROVE, "approve-clerk", "CLERK", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("FINANCE");
        assertThat(appService.get(created.getInvoiceNo()).getStatus()).isEqualTo(InvoiceState.PENDING_REVIEW);

        InvoiceView issued = fire(created.getInvoiceNo(), InvoiceEvent.APPROVE, "approve-finance", "FINANCE", null);
        assertThat(issued.getStatus()).isEqualTo(InvoiceState.ISSUED);
    }

    @Test
    @DisplayName("驳回后可以重新提交")
    void rejectThenResubmit() {
        InvoiceView created = appService.create("咨询费", new BigDecimal("500"));
        fire(created.getInvoiceNo(), InvoiceEvent.SUBMIT, "submit-3", "CLERK", null);
        InvoiceView rejected = fire(created.getInvoiceNo(), InvoiceEvent.REJECT, "reject-3", "CLERK", "抬头不一致");
        assertThat(rejected.getStatus()).isEqualTo(InvoiceState.REJECTED);
        assertThat(rejected.getRejectReason()).isEqualTo("抬头不一致");
        InvoiceView pending = fire(created.getInvoiceNo(), InvoiceEvent.RESUBMIT, "resubmit-3", "CLERK", null);
        assertThat(pending.getStatus()).isEqualTo(InvoiceState.PENDING_REVIEW);
        assertThat(pending.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("草稿不能直接红冲")
    void draftCannotRedFlush() {
        InvoiceView created = appService.create("草稿发票", new BigDecimal("10"));
        assertThatThrownBy(() -> fire(created.getInvoiceNo(), InvoiceEvent.RED_FLUSH, "flush-draft", "CLERK", null))
                .isInstanceOf(BizException.class);
    }

    private InvoiceView fire(String invoiceNo, InvoiceEvent event, String eventId, String role, String remark) {
        InvoiceEventCommand command = new InvoiceEventCommand();
        command.setEvent(event);
        command.setOperator("tester");
        command.setEventId(eventId);
        command.setRole(role);
        command.setRemark(remark);
        return appService.fire(invoiceNo, command);
    }
}
