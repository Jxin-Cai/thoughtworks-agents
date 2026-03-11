package com.thoughtworks.agents.infr.github;

import com.thoughtworks.agents.domain.exception.GitHubApiException;
import com.thoughtworks.agents.domain.exception.GitHubMergeConflictException;
import com.thoughtworks.agents.domain.github.acl.GitHubAclService;
import com.thoughtworks.agents.domain.github.model.Branch;
import com.thoughtworks.agents.domain.github.model.OAuthToken;
import com.thoughtworks.agents.domain.github.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GitHubAclServiceImpl implements GitHubAclService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAclServiceImpl.class);
    private static final String API_BASE_URL = "https://api.github.com";
    private static final String OAUTH_BASE_URL = "https://github.com";

    private final RestTemplate restTemplate;
    private final GitHubOAuthProperties oauthProperties;

    public GitHubAclServiceImpl(RestTemplateBuilder restTemplateBuilder, GitHubOAuthProperties oauthProperties) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(5000))
                .readTimeout(Duration.ofMillis(10000))
                .build();
        this.oauthProperties = oauthProperties;
    }

    @Override
    public List<Repository> listRepositories(OAuthToken token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(
                    API_BASE_URL + "/user/repos?sort=updated&per_page=100",
                    HttpMethod.GET, entity, List.class);

            List<Map<String, Object>> repos = response.getBody();
            if (repos == null) {
                return Collections.emptyList();
            }
            return repos.stream()
                    .map(repo -> new Repository(
                            (String) repo.get("full_name"),
                            (String) repo.get("default_branch"),
                            (String) repo.get("clone_url"),
                            Boolean.TRUE.equals(repo.get("private"))
                    ))
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new GitHubApiException("Unauthorized");
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException("Failed to list repositories: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Branch> listBranches(OAuthToken token, String repositoryFullName) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(
                    API_BASE_URL + "/repos/" + repositoryFullName + "/branches?per_page=100",
                    HttpMethod.GET, entity, List.class);

            List<Map<String, Object>> branches = response.getBody();
            if (branches == null) {
                return Collections.emptyList();
            }
            return branches.stream()
                    .map(branch -> {
                        Map<String, Object> commit = (Map<String, Object>) branch.get("commit");
                        String sha = commit != null ? (String) commit.get("sha") : null;
                        return new Branch((String) branch.get("name"), sha);
                    })
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException.NotFound e) {
            throw new GitHubApiException("Repository not found");
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException("Failed to list branches: " + e.getMessage(), e);
        }
    }

    @Override
    public Branch createBranch(OAuthToken token, String repositoryFullName,
                               String branchName, String sourceBranch) {
        try {
            HttpHeaders headers = createAuthHeaders(token);

            HttpEntity<Void> getEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> refResponse = restTemplate.exchange(
                    API_BASE_URL + "/repos/" + repositoryFullName + "/git/ref/heads/" + sourceBranch,
                    HttpMethod.GET, getEntity, Map.class);

            Map<String, Object> refBody = refResponse.getBody();
            Map<String, Object> objectMap = (Map<String, Object>) refBody.get("object");
            String sha = (String) objectMap.get("sha");

            Map<String, String> createBody = new HashMap<>();
            createBody.put("ref", "refs/heads/" + branchName);
            createBody.put("sha", sha);

            HttpEntity<Map<String, String>> postEntity = new HttpEntity<>(createBody, headers);
            restTemplate.exchange(
                    API_BASE_URL + "/repos/" + repositoryFullName + "/git/refs",
                    HttpMethod.POST, postEntity, Map.class);

            return new Branch(branchName, sha);

        } catch (HttpClientErrorException.UnprocessableEntity e) {
            throw new GitHubApiException("Branch already exists");
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException("Failed to create branch: " + e.getMessage(), e);
        }
    }

    @Override
    public void mergeBranch(OAuthToken token, String repositoryFullName,
                            String headBranch, String baseBranch) {
        try {
            HttpHeaders headers = createAuthHeaders(token);

            Map<String, String> body = new HashMap<>();
            body.put("base", baseBranch);
            body.put("head", headBranch);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            restTemplate.exchange(
                    API_BASE_URL + "/repos/" + repositoryFullName + "/merges",
                    HttpMethod.POST, entity, Map.class);

        } catch (HttpClientErrorException.Conflict e) {
            throw new GitHubMergeConflictException(repositoryFullName, headBranch, baseBranch);
        } catch (HttpClientErrorException.NotFound e) {
            throw new GitHubApiException("Repository or branch not found");
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException("Failed to merge branch: " + e.getMessage(), e);
        }
    }

    @Override
    public OAuthToken exchangeCodeForToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("client_id", oauthProperties.getClientId());
            body.put("client_secret", oauthProperties.getClientSecret());
            body.put("code", code);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    OAUTH_BASE_URL + "/login/oauth/access_token",
                    HttpMethod.POST, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new GitHubApiException("Empty response from GitHub OAuth");
            }
            if (responseBody.containsKey("error")) {
                throw new GitHubApiException("OAuth error: " + responseBody.get("error_description"));
            }

            return new OAuthToken(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("token_type"),
                    (String) responseBody.get("scope"),
                    LocalDateTime.now()
            );
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException("Failed to exchange code for token: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAuthenticatedUsername(OAuthToken token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_BASE_URL + "/user",
                    HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new GitHubApiException("Empty response from GitHub API");
            }
            return (String) body.get("login");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new GitHubApiException("Unauthorized");
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException("Failed to get authenticated username: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createAuthHeaders(OAuthToken token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.github.v3+json")));
        return headers;
    }
}
