package sergio.task.githubrepository.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sergio.task.githubrepository.model.PopularRepositoriesResponse;
import sergio.task.githubrepository.service.PopularityService;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/repositories")
@RequiredArgsConstructor
@Validated
@Tag(name = "GitHub Scorer", description = "Calculates the popularity of the github repositories")
public class PopularityController {

    private final PopularityService popularityService;

    @GetMapping("/popular")
    @Operation(summary = "Search and score popular GitHub repositories",
            description = "Searches for GitHub repositories created after a specific date and in a given language," +
                    "calculating their popularity based on stars and forks.")
    public ResponseEntity<PopularRepositoriesResponse> getPopularRepositories(
            @Parameter(description = "Earliest created date dd-MM-yyy)", example = "01-01-2023", required = true)
            @RequestParam("since")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            LocalDate since,

            @Parameter(description = "Programming language", example = "java", required = true)
            @RequestParam("language")
            @NotBlank
            String language
    ) {
        var results = popularityService.getPopularRepositories(since.toString(), language);

        if (results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        var response = new PopularRepositoriesResponse(results.size(), results);
        return ResponseEntity.ok(response);
    }
}