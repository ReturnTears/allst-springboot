package com.allst.springboot.contract.api;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.contract.domain.ContractEventCommand;
import com.allst.springboot.contract.service.ContractAppService;
import com.allst.springboot.contract.service.ContractView;
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
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractAppService appService;

    public ContractController(ContractAppService appService) {
        this.appService = appService;
    }

    @PostMapping
    public Result<ContractView> create(@Valid @RequestBody CreateContractRequest request) {
        return Result.ok(appService.create(request.getTitle(), request.getAmount()));
    }

    @GetMapping("/{contractNo}")
    public Result<ContractView> get(@PathVariable String contractNo) {
        return Result.ok(appService.get(contractNo));
    }

    @GetMapping("/{contractNo}/logs")
    public Result<List<TransitionLogView>> logs(@PathVariable String contractNo) {
        return Result.ok(appService.logs(contractNo));
    }

    @PostMapping("/{contractNo}/events")
    public Result<ContractView> fire(@PathVariable String contractNo, @Valid @RequestBody ContractEventRequest request) {
        ContractEventCommand command = new ContractEventCommand();
        command.setEvent(request.getEvent());
        command.setOperator(request.getOperator());
        command.setEventId(request.getEventId());
        command.setRemark(request.getRemark());
        return Result.ok(appService.fire(contractNo, command));
    }
}
