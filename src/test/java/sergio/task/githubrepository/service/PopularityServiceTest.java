package sergio.task.githubrepository.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sergio.task.githubrepository.external.GithubClient;
import sergio.task.githubrepository.external.model.GithubRepositoryDto;
import sergio.task.githubrepository.external.model.GithubSearchResponse;
import sergio.task.githubrepository.model.RepositoryScoreResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularityServiceTest {

    @Mock
    private GithubClient githubClient;
    @Mock
    private PopularityCalculator calculator;

    @InjectMocks
    private PopularityService popularityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(popularityService, "maxPagesToFetch", 5);
    }

    @Test
    @DisplayName("Should return sorted list and handle multiple pages calculation")
    void getPopularRepositories_ShouldCalculateAndSortCorrectly() {
        String since = "2023-01-01";
        String language = "java";

        GithubRepositoryDto repo1 = new GithubRepositoryDto("r1", "user/r1", 10, 5, LocalDateTime.now(), "Java", "url");
        GithubRepositoryDto repo2 = new GithubRepositoryDto("r2", "user/r2", 20, 10, LocalDateTime.now(), "Java", "url");

        when(githubClient.searchRepositories(anyString(), anyString(), eq(1), anyInt()))
                .thenReturn(new GithubSearchResponse(150, List.of(repo1)));

        when(githubClient.searchRepositories(anyString(), anyString(), eq(2), anyInt()))
                .thenReturn(new GithubSearchResponse(150, List.of(repo2)));

        when(calculator.calculateScore(any())).thenReturn(10.0, 50.0);

        List<RepositoryScoreResponse> result = popularityService.getPopularRepositories(since, language);

        assertEquals(2, result.size(), "Should have collected repositories from all fetched pages");
        assertEquals("user/r2", result.getFirst().fullName(), "The result should be sorted by score descending");

        verify(githubClient, times(2)).searchRepositories(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should return empty list when GitHub returns no results")
    void getPopularRepositories_EmptyResponse() {
        when(githubClient.searchRepositories(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GithubSearchResponse(0, List.of()));

        List<RepositoryScoreResponse> result = popularityService.getPopularRepositories("2023-01-01", "java");

        assertTrue(result.isEmpty());
    }
}