package com.thoughtworks.agents.application.github;

import com.thoughtworks.agents.application.exception.BusinessException;
import com.thoughtworks.agents.domain.github.acl.GitHubAclService;
import com.thoughtworks.agents.domain.github.model.GitHubIntegration;
import com.thoughtworks.agents.domain.github.model.OAuthToken;
import com.thoughtworks.agents.domain.github.model.Repository;
import com.thoughtworks.agents.domain.github.repository.GitHubIntegrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GitHubApplicationService {

    private final GitHubIntegrationRepository gitHubIntegrationRepository;
    private final GitHubAclService gitHubAclService;

    public GitHubApplicationService(GitHubIntegrationRepository gitHubIntegrationRepository,
                                    GitHubAclService gitHubAclService) {
        this.gitHubIntegrationRepository = gitHubIntegrationRepository;
        this.gitHubAclService = gitHubAclService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOAuthCallback(HandleOAuthCallbackCommand command) {
        OAuthToken token = gitHubAclService.exchangeCodeForToken(command.getCode());
        String username = gitHubAclService.getAuthenticatedUsername(token);

        GitHubIntegration integration = gitHubIntegrationRepository.find()
                .orElseGet(GitHubIntegration::create);

        integration.authenticate(token, username);
        gitHubIntegrationRepository.save(integration);
    }

    @Transactional(readOnly = true)
    public List<RepositoryDTO> listRepositories() {
        GitHubIntegration integration = gitHubIntegrationRepository.find()
                .orElseThrow(() -> new BusinessException("请先完成 GitHub OAuth 认证"));

        if (!integration.isAuthenticated()) {
            throw new BusinessException("请先完成 GitHub OAuth 认证");
        }

        OAuthToken token = integration.getRequiredToken();
        List<Repository> repos = gitHubAclService.listRepositories(token);
        return repos.stream().map(RepositoryDTO::from).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public MergeResultDTO mergeBranch(String repositoryFullName, String headBranch, String baseBranch) {
        GitHubIntegration integration = gitHubIntegrationRepository.find()
                .orElseThrow(() -> new BusinessException("请先完成 GitHub OAuth 认证"));

        OAuthToken token = integration.getRequiredToken();
        gitHubAclService.mergeBranch(token, repositoryFullName, headBranch, baseBranch);
        return MergeResultDTO.success(repositoryFullName, headBranch, baseBranch);
    }
}
