package com.allst.springboot.contract;

import com.allst.springboot.common.async.AsyncTaskRecorder;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.contract.domain.ContractEvent;
import com.allst.springboot.contract.domain.ContractEventCommand;
import com.allst.springboot.contract.domain.ContractState;
import com.allst.springboot.contract.service.ContractAppService;
import com.allst.springboot.contract.service.ContractView;
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
class ContractReviewFlowTest {

    @Autowired
    private ContractAppService appService;
    @Autowired
    private AsyncTaskRecorder recorder;

    @Test
    @DisplayName("法务和财务都通过后才能签署")
    void bothTracksJoinThenSign() {
        ContractView created = appService.create("年度采购框架", new BigDecimal("80000"));
        ContractView reviewing = fire(created.getContractNo(), ContractEvent.SUBMIT, "submit-1");
        assertThat(reviewing.getPhase()).isEqualTo(ContractState.REVIEWING);
        assertThat(reviewing.getLegalStatus()).isEqualTo(ContractState.LEGAL_PENDING);
        assertThat(reviewing.getFinanceStatus()).isEqualTo(ContractState.FINANCE_PENDING);
        awaitTask("legal-review:" + created.getContractNo());
        awaitTask("finance-review:" + created.getContractNo());

        ContractView legalPassed = fire(created.getContractNo(), ContractEvent.LEGAL_PASS, "legal-1");
        assertThat(legalPassed.getPhase()).isEqualTo(ContractState.REVIEWING);
        assertThat(legalPassed.getLegalStatus()).isEqualTo(ContractState.LEGAL_PASSED);
        assertThat(legalPassed.getFinanceStatus()).isEqualTo(ContractState.FINANCE_PENDING);

        ContractView joined = fire(created.getContractNo(), ContractEvent.FINANCE_PASS, "finance-1");
        assertThat(joined.getPhase()).isEqualTo(ContractState.WAIT_SIGN);
        assertThat(joined.getLegalStatus()).isEqualTo(ContractState.LEGAL_PASSED);
        assertThat(joined.getFinanceStatus()).isEqualTo(ContractState.FINANCE_PASSED);
        assertThat(recorder.countDone("legal-review:" + created.getContractNo())).isEqualTo(1);

        ContractView signed = fire(created.getContractNo(), ContractEvent.SIGN, "sign-1");
        assertThat(signed.getPhase()).isEqualTo(ContractState.SIGNED);
        assertThat(signed.getAllowedEvents()).isEmpty();
    }

    @Test
    @DisplayName("财务先通过，法务后通过，同样汇合")
    void financeFirstStillJoins() {
        ContractView created = appService.create("补充协议", new BigDecimal("1200"));
        fire(created.getContractNo(), ContractEvent.SUBMIT, "submit-2");
        fire(created.getContractNo(), ContractEvent.FINANCE_PASS, "finance-2");
        ContractView joined = fire(created.getContractNo(), ContractEvent.LEGAL_PASS, "legal-2");
        assertThat(joined.getPhase()).isEqualTo(ContractState.WAIT_SIGN);
    }

    @Test
    @DisplayName("任一侧驳回后合同结束，另一侧不能再审")
    void oneRejectStopsTheOtherTrack() {
        ContractView created = appService.create("一次性采购", new BigDecimal("300"));
        fire(created.getContractNo(), ContractEvent.SUBMIT, "submit-3");
        ContractView rejected = fire(created.getContractNo(), ContractEvent.LEGAL_REJECT, "legal-no");
        assertThat(rejected.getPhase()).isEqualTo(ContractState.REJECTED);
        assertThat(rejected.getLegalStatus()).isEqualTo(ContractState.LEGAL_REJECTED);
        assertThatThrownBy(() -> fire(created.getContractNo(), ContractEvent.FINANCE_PASS, "finance-too-late"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("草稿可以取消")
    void cancelDraft() {
        ContractView created = appService.create("未提交合同", new BigDecimal("100"));
        ContractView cancelled = fire(created.getContractNo(), ContractEvent.CANCEL, "cancel-1");
        assertThat(cancelled.getPhase()).isEqualTo(ContractState.CANCELLED);
    }

    private ContractView fire(String contractNo, ContractEvent event, String eventId) {
        ContractEventCommand command = new ContractEventCommand();
        command.setEvent(event);
        command.setOperator("tester");
        command.setEventId(eventId);
        return appService.fire(contractNo, command);
    }

    private void awaitTask(String taskName) {
        long deadline = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < deadline) {
            if (recorder.countDone(taskName) > 0) {
                return;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(recorder.snapshot()).contains("done:" + taskName);
    }
}
