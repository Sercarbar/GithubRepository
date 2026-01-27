package sergio.task.githubrepository.model;

public record RepositoryScoreResponse(
        String fullName,
        int stars,
        int forks,
        String language,
        double popularityScore,
        String url
) {
}