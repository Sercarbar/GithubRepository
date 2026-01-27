package sergio.task.githubrepository.external;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sergio.task.githubrepository.external.model.GithubSearchResponse;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubClient {

    private final RestClient restClient;

    @CircuitBreaker(name = "githubSearch", fallbackMethod = "fallbackSearch")
    public GithubSearchResponse searchRepositories(String date, String language, int page, int itemsPerPage) {
        // Query pattern from GitHub: "created:>YYYY-MM-DD language:xxx"
        String query = String.format("created:>%s language:%s", date, language);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/repositories")
                        .queryParam("q", query)
                        .queryParam("sort", "stars")
                        .queryParam("order", "desc")
                        .queryParam("per_page", itemsPerPage)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(GithubSearchResponse.class);
    }

    public GithubSearchResponse fallbackSearch(String date, String language, int page, int itemsPerPage, Throwable e) {
        log.error("Circuit Breaker triggered for GitHub search. Params: [date={}, lang={}, page={}, itemsPerPage={}]. Reason: {}",
                date, language, page, itemsPerPage, e.getMessage());

        return new GithubSearchResponse(0, Collections.emptyList());
    }
}
