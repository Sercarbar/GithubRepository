package sergio.task.githubrepository.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import sergio.task.githubrepository.external.GithubClient;
import sergio.task.githubrepository.external.model.GithubRepositoryDto;
import sergio.task.githubrepository.model.RepositoryScoreResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityService {

    private static final int ITEMS_PER_PAGE = 100;//GitHub documentation says max 100
    private final GithubClient githubClient;
    private final PopularityCalculator calculator;
    @Value("${app.github.max-pages-to-fetch:5}")
    private int maxPagesToFetch;

    // I'm caching to improve performance and avoid hitting rate limits established by GitHub API
    // We could implement a more sophisticated cache by saving to Redis or similar to avoid losing
    // cache on app restart and shared cache with multiple instances (Distributed Cache)
    @Cacheable(value = "github-repos", key = "#createdAfter + '-' + #language")
    public List<RepositoryScoreResponse> getPopularRepositories(String createdAfter, String language) {

        log.info("Starting search for: {} from {}", language, createdAfter);

        // 1. Fetch of Page 1 (to determine total results)
        var firstPage = githubClient.searchRepositories(createdAfter, language, 1, ITEMS_PER_PAGE);

        if (firstPage == null || firstPage.items().isEmpty()) {
            return List.of();
        }

        List<GithubRepositoryDto> allRepos = Collections.synchronizedList(new ArrayList<>(firstPage.items()));
        var totalCount = firstPage.totalCount();

        // 2. Calculate pages to fetch
        int totalPages = (int) Math.ceil((double) totalCount / ITEMS_PER_PAGE);
        int pagesToFetch = Math.min(totalPages, maxPagesToFetch);

        // 3. Parallel Fetch Remaining Pages starting from page 2
        if (pagesToFetch > 1) {
            fetchRestOfPagesInParallel(createdAfter, language, pagesToFetch, allRepos);
        }

        // 4. Map list of repos, calculate score and sort DESC
        return allRepos.stream()
                .map(this::mapToRepositoryScoreResponse)
                .sorted((repo1, repo2) -> Double.compare(repo2.popularityScore(), repo1.popularityScore()))
                .toList();
    }

    private void fetchRestOfPagesInParallel(String date, String language, int endPage, List<GithubRepositoryDto> accumulatedResults) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<CompletableFuture<List<GithubRepositoryDto>>> futures = IntStream.rangeClosed(2, endPage)
                    .mapToObj(page -> CompletableFuture.supplyAsync(() -> {
                        var response = githubClient.searchRepositories(date, language, page, ITEMS_PER_PAGE);
                        return response != null ? response.items() : Collections.<GithubRepositoryDto>emptyList();
                    }, executor).exceptionally(ex -> {
                        log.error("Error fetching page {}: {}", page, ex.getMessage());
                        return Collections.emptyList();
                    }))
                    .toList();

            futures.stream()
                    .map(CompletableFuture::join)
                    .forEach(accumulatedResults::addAll);
        }
    }

    private RepositoryScoreResponse mapToRepositoryScoreResponse(GithubRepositoryDto repo) {
        return new RepositoryScoreResponse(
                repo.fullName(),
                repo.stars(),
                repo.forks(),
                repo.language(),
                calculator.calculateScore(repo),
                repo.url()
        );
    }
}
