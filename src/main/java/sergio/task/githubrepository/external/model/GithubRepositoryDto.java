package sergio.task.githubrepository.external.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GithubRepositoryDto(
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("stargazers_count") int stars,
        @JsonProperty("forks_count") int forks,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        String language,
        @JsonProperty("html_url") String url
) {}
