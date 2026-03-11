package com.thoughtworks.agents.infr.repository.github;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thoughtworks.agents.domain.github.model.GitHubIntegration;
import com.thoughtworks.agents.domain.github.model.GitHubIntegrationId;
import com.thoughtworks.agents.domain.github.model.OAuthToken;
import com.thoughtworks.agents.domain.github.repository.GitHubIntegrationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GitHubIntegrationRepositoryImpl implements GitHubIntegrationRepository {

    private final GitHubIntegrationMapper gitHubIntegrationMapper;

    public GitHubIntegrationRepositoryImpl(GitHubIntegrationMapper gitHubIntegrationMapper) {
        this.gitHubIntegrationMapper = gitHubIntegrationMapper;
    }

    @Override
    public void save(GitHubIntegration integration) {
        GitHubIntegrationPO po = toPO(integration);
        GitHubIntegrationPO existing = gitHubIntegrationMapper.selectById(po.getId());
        if (existing != null) {
            gitHubIntegrationMapper.updateById(po);
        } else {
            gitHubIntegrationMapper.insert(po);
        }
    }

    @Override
    public Optional<GitHubIntegration> find() {
        LambdaQueryWrapper<GitHubIntegrationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 1");
        GitHubIntegrationPO po = gitHubIntegrationMapper.selectOne(wrapper);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    private GitHubIntegrationPO toPO(GitHubIntegration integration) {
        GitHubIntegrationPO po = new GitHubIntegrationPO();
        po.setId(integration.getId().getValue());
        OAuthToken token = integration.getOauthToken();
        if (token != null) {
            po.setAccessToken(token.getAccessToken());
            po.setTokenType(token.getTokenType());
            po.setScope(token.getScope());
            po.setTokenCreatedAt(token.getCreatedAt());
        }
        po.setAuthenticatedUser(integration.getAuthenticatedUser());
        po.setCreatedAt(integration.getCreatedAt());
        po.setUpdatedAt(integration.getUpdatedAt());
        return po;
    }

    private GitHubIntegration toDomain(GitHubIntegrationPO po) {
        OAuthToken oauthToken = po.getAccessToken() != null
                ? new OAuthToken(po.getAccessToken(), po.getTokenType(), po.getScope(), po.getTokenCreatedAt())
                : null;
        return GitHubIntegration.reconstitute(
                new GitHubIntegrationId(po.getId()),
                oauthToken,
                po.getAuthenticatedUser(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
