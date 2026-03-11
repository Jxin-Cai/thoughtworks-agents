package com.thoughtworks.agents.ohs.controller;

import com.thoughtworks.agents.application.ccsession.CCSessionApplicationService;
import com.thoughtworks.agents.application.ccsession.CCSessionDTO;
import com.thoughtworks.agents.application.ccsession.TerminateCCSessionCommand;
import com.thoughtworks.agents.ohs.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cc-sessions")
public class CCSessionController {

    private final CCSessionApplicationService ccSessionApplicationService;

    public CCSessionController(CCSessionApplicationService ccSessionApplicationService) {
        this.ccSessionApplicationService = ccSessionApplicationService;
    }

    @GetMapping("/{sessionId}")
    public Result<CCSessionDTO> getSession(@PathVariable String sessionId) {
        CCSessionDTO ccSessionDTO = ccSessionApplicationService.getSession(sessionId);
        return Result.success(ccSessionDTO);
    }

    @GetMapping("/active")
    public Result<List<CCSessionDTO>> getActiveSessions() {
        List<CCSessionDTO> ccSessionDTOList = ccSessionApplicationService.getActiveSessions();
        return Result.success(ccSessionDTOList);
    }

    @PostMapping("/{sessionId}/terminate")
    public Result<Void> terminateSession(@PathVariable String sessionId) {
        TerminateCCSessionCommand command = TerminateCCSessionCommand.builder()
                .sessionId(sessionId)
                .build();
        ccSessionApplicationService.terminateCCSession(command);
        return Result.success();
    }
}
