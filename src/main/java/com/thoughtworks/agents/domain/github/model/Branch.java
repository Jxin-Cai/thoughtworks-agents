package com.thoughtworks.agents.domain.github.model;

import java.util.Objects;

public class Branch {

    private final String name;
    private final String sha;

    public Branch(String name, String sha) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name must not be blank");
        }
        this.name = name;
        this.sha = sha;
    }

    public String getName() {
        return name;
    }

    public String getSha() {
        return sha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Branch branch = (Branch) o;
        return Objects.equals(name, branch.name) && Objects.equals(sha, branch.sha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sha);
    }
}
