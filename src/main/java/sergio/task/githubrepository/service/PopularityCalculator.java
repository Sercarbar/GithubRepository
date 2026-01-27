package sergio.task.githubrepository.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sergio.task.githubrepository.external.model.GithubRepositoryDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class PopularityCalculator {

    private final ScoringProperties props;

    public double calculateScore(GithubRepositoryDto repo) {
        if (repo == null) return 0.0;

        double baseScore = Math.log10(repo.stars() + 1) * props.starsWeight();
        baseScore += (Math.log10(repo.forks() + 1) * props.forksWeight());

        double freshnessMultiplier = calculateFreshnessMultiplier(repo.updatedAt());

        return baseScore * freshnessMultiplier;
    }

    private double calculateFreshnessMultiplier(LocalDateTime updatedAt) {
        if (updatedAt == null) return props.freshness().defaultMultiplier();

        long daysOld = ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());

        if (daysOld <= props.freshness().veryRecentDays()) {
            return props.freshness().boostVeryRecent();
        }
        if (daysOld <= props.freshness().recentDays()) {
            return props.freshness().boostRecent();
        }
        if (daysOld > props.freshness().oldDays()) {
            return props.freshness().penaltyOld();
        }

        return props.freshness().defaultMultiplier();
    }
}