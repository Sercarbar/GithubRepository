package sergio.task.githubrepository.model;

import java.util.List;

public record PopularRepositoriesResponse(
        int count,
        List<RepositoryScoreResponse> items
) {
}
