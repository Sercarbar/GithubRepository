package sergio.task.githubrepository.service;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sergio.task.githubrepository.external.GithubClient;
import sergio.task.githubrepository.external.model.GithubRepositoryDto;
import sergio.task.githubrepository.external.model.GithubSearchResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PopularityServiceCacheTest {

    @Autowired
    private PopularityService popularityService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private GithubClient githubClient;

    @Test
    @DisplayName("Cache - Should invoke GithubClient only once when multiple calls are made with same parameters")
    void getPopularRepositories_ShouldUseCache() {
        GithubRepositoryDto repo = new GithubRepositoryDto("r1", "user/r1", 10, 5, LocalDateTime.now(), "java", "url");
        GithubSearchResponse mockResponse = new GithubSearchResponse(1, List.of(repo));

        when(githubClient.searchRepositories(anyString(), anyString(), anyInt(), anyInt())).thenReturn(mockResponse);

        popularityService.getPopularRepositories("2023-01-01", "java");
        popularityService.getPopularRepositories("2023-01-01", "java");
        popularityService.getPopularRepositories("2023-01-01", "java");

        verify(githubClient, times(1)).searchRepositories(eq("2023-01-01"), eq("java"), anyInt(), anyInt());
    }
}