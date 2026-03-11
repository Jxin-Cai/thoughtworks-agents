package com.thoughtworks.agents.domain.github.repository;

import com.thoughtworks.agents.domain.github.model.GitHubIntegration;

import java.util.Optional;

public interface GitHubIntegrationRepository {

    void save(GitHubIntegration integration);

    Optional<GitHubIntegration> find();
}
