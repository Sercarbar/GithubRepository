package sergio.task.githubrepository.external.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GithubSearchResponse(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("items") List<GithubRepositoryDto> items
) {
}
