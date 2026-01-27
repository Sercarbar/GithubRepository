package sergio.task.githubrepository.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.popularity.scoring")
public record ScoringProperties(
        double starsWeight,
        double forksWeight,
        Freshness freshness
) {
    public record Freshness(
            int veryRecentDays,
            int recentDays,
            int oldDays,
            double boostVeryRecent,
            double boostRecent,
            double penaltyOld,
            double defaultMultiplier
    ) {
    }
}
