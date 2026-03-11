package com.thoughtworks.agents.ohs.controller;

import com.thoughtworks.agents.application.github.GitHubApplicationService;
import com.thoughtworks.agents.application.github.HandleOAuthCallbackCommand;
import com.thoughtworks.agents.application.github.MergeResultDTO;
import com.thoughtworks.agents.application.github.RepositoryDTO;
import com.thoughtworks.agents.ohs.common.Result;
import com.thoughtworks.agents.ohs.dto.MergeBranchRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubApplicationService gitHubApplicationService;

    public GitHubController(GitHubApplicationService gitHubApplicationService) {
        this.gitHubApplicationService = gitHubApplicationService;
    }

    @GetMapping("/oauth/callback")
    public Result<Void> handleOAuthCallback(@RequestParam String code) {
        HandleOAuthCallbackCommand command = HandleOAuthCallbackCommand.builder()
                .code(code)
                .build();
        gitHubApplicationService.handleOAuthCallback(command);
        return Result.success();
    }

    @GetMapping("/repositories")
    public Result<List<RepositoryDTO>> listRepositories() {
        List<RepositoryDTO> repositoryDTOList = gitHubApplicationService.listRepositories();
        return Result.success(repositoryDTOList);
    }

    @PostMapping("/merge")
    public Result<MergeResultDTO> mergeBranch(@Validated @RequestBody MergeBranchRequest request) {
        MergeResultDTO mergeResultDTO = gitHubApplicationService.mergeBranch(
                request.getRepositoryFullName(),
                request.getHeadBranch(),
                request.getBaseBranch()
        );
        return Result.success(mergeResultDTO);
    }
}
