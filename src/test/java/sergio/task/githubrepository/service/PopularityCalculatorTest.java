package sergio.task.githubrepository.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sergio.task.githubrepository.external.model.GithubRepositoryDto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopularityCalculatorTest {

    private PopularityCalculator calculator;

    @BeforeEach
    void setUp() {
        var freshnessProps = new ScoringProperties.Freshness(3, 14, 365, 1.5, 1.2, 0.5, 1.0);
        var props = new ScoringProperties(1.0, 1.5, freshnessProps);
        this.calculator = new PopularityCalculator(props);
    }

    @Test
    @DisplayName("Should calculate base score using logarithms (Old repo > 1 year)")
    void shouldCalculateBaseScoreWithPenalty() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo1", "user/repo1",
                100, 10,
                LocalDateTime.now().minusDays(400),
                "java", "http://url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(1.78, score, 0.01);
    }

    @Test
    @DisplayName("Should apply freshness multiplier (New repo < 3 days old)")
    void shouldApplyFreshnessBoost() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo2", "user/repo2",
                1000, 0,
                LocalDateTime.now().minusDays(2),
                "java", "http://url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(4.5, score, 0.01);
    }

    @Test
    @DisplayName("Should apply recent multiplier (Repo between 3 and 14 days old)")
    void shouldApplyRecentBoost() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo-recent", "user/recent",
                100, 0,
                LocalDateTime.now().minusDays(10),
                "java", "url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(2.40, score, 0.01);
    }

    @Test
    @DisplayName("Should apply default multiplier (Repo between 14 days and 1 year)")
    void shouldApplyDefaultMultiplier() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo-normal", "user/normal",
                1000, 0,
                LocalDateTime.now().minusDays(100),
                "java", "url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(3.0, score, 0.01);
    }

    @Test
    @DisplayName("Should include weighted forks in calculation")
    void shouldCalculateScoreWithForksOnly() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo-forks", "user/forks",
                0, 100,
                LocalDateTime.now().minusDays(20),
                "java", "url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(3.00, score, 0.01);
    }

    @Test
    @DisplayName("Should use default multiplier when date is null")
    void shouldHandleNullDate() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo-nodate", "user/nodate",
                10, 0, null, "java", "url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(1.04, score, 0.01);
    }

    @Test
    @DisplayName("Should handle exact boundary days correctly")
    void shouldHandleBoundaryDays() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo-boundary", "user/boundary",
                100, 0,
                LocalDateTime.now().minusDays(14),
                "java", "url"
        );

        double score = calculator.calculateScore(repo);

        assertEquals(2.40, score, 0.01);
    }

    @Test
    @DisplayName("Should return 0 if input is null or empty")
    void shouldReturnZeroForEmpty() {
        GithubRepositoryDto repo = new GithubRepositoryDto(
                "repo3", "user/repo3", 0, 0, null, "java", "url"
        );

        assertEquals(0.0, calculator.calculateScore(repo));
        assertEquals(0.0, calculator.calculateScore(null));
    }
}