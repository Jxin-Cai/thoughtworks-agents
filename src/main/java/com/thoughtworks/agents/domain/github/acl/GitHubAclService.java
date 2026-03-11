package com.thoughtworks.agents.domain.github.acl;

import com.thoughtworks.agents.domain.github.model.Branch;
import com.thoughtworks.agents.domain.github.model.OAuthToken;
import com.thoughtworks.agents.domain.github.model.Repository;

import java.util.List;

public interface GitHubAclService {

    List<Repository> listRepositories(OAuthToken token);

    List<Branch> listBranches(OAuthToken token, String repositoryFullName);

    Branch createBranch(OAuthToken token, String repositoryFullName,
                        String branchName, String sourceBranch);

    void mergeBranch(OAuthToken token, String repositoryFullName,
                     String headBranch, String baseBranch);

    OAuthToken exchangeCodeForToken(String code);

    String getAuthenticatedUsername(OAuthToken token);
}
