package sergio.task.githubrepository.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sergio.task.githubrepository.external.model.GithubRepositoryDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class PopularityCalculator {

    private final ScoringProperties scoringProps;

    public double calculateScore(GithubRepositoryDto repo) {
        if (repo == null) return 0.0;

        double baseScore = Math.log10(repo.stars() + 1) * scoringProps.starsWeight();
        baseScore += (Math.log10(repo.forks() + 1) * scoringProps.forksWeight());

        double freshnessMultiplier = calculateFreshnessMultiplier(repo.updatedAt());

        return baseScore * freshnessMultiplier;
    }

    private double calculateFreshnessMultiplier(LocalDateTime updatedAt) {
        if (updatedAt == null) return scoringProps.freshness().defaultMultiplier();

        long daysOld = ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());

        if (daysOld <= scoringProps.freshness().veryRecentDays()) {
            return scoringProps.freshness().boostVeryRecent();
        }
        if (daysOld <= scoringProps.freshness().recentDays()) {
            return scoringProps.freshness().boostRecent();
        }
        if (daysOld > scoringProps.freshness().oldDays()) {
            return scoringProps.freshness().penaltyOld();
        }

        return scoringProps.freshness().defaultMultiplier();
    }
}