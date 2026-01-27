package sergio.task.githubrepository.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import sergio.task.githubrepository.configuration.GithubRepositoryConfig;
import sergio.task.githubrepository.external.model.GithubSearchResponse;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(GithubClient.class)
@Import(GithubRepositoryConfig.class)
class GithubClientTest {

    private static final int ITEMS_PER_PAGE = 100;
    @Autowired
    private GithubClient githubClient;
    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    @DisplayName("Should correctly parse a successful response from GitHub")
    void searchRepositories_Success() {
        String mockJsonResponse = """
                    {
                        "total_count": 1,
                        "items": [
                            {
                                "name": "test-repo",
                                "full_name": "sergio/test-repo",
                                "stargazers_count": 100,
                                "forks_count": 20,
                                "updated_at": "2023-10-01T12:00:00Z",
                                "language": "Java",
                                "html_url": "http://github.com/sergio/test-repo"
                            }
                        ]
                    }
                """;

        mockServer.expect(requestTo(containsString("q=created:%3E2023-01-01")))
                .andExpect(requestTo(containsString("language:java")))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        GithubSearchResponse response = githubClient.searchRepositories("2023-01-01", "java", 1, ITEMS_PER_PAGE);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals("sergio/test-repo", response.items().getFirst().fullName());
        assertEquals(100, response.items().getFirst().stars());
    }

    @Test
    @DisplayName("Should handle empty results (total_count: 0)")
    void searchRepositories_EmptyResults() {
        String emptyResponse = "{\"total_count\": 0, \"items\": []}";

        mockServer.expect(requestTo(containsString("language:python")))
                .andRespond(withSuccess(emptyResponse, MediaType.APPLICATION_JSON));

        GithubSearchResponse response = githubClient.searchRepositories("2023-01-01", "python", 1, ITEMS_PER_PAGE);

        assertNotNull(response);
        assertTrue(response.items().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when GitHub returns 403 Forbidden (Rate Limit)")
    void searchRepositories_RateLimitError() {
        mockServer.expect(requestTo(containsString("language:java")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(HttpClientErrorException.class, () ->
                githubClient.searchRepositories("2023-01-01", "java", 1, ITEMS_PER_PAGE)
        );
    }

    @Test
    @DisplayName("Should throw exception when GitHub returns 500 Server Error")
    void searchRepositories_ServerError() {
        mockServer.expect(requestTo(containsString("language:java")))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () ->
                githubClient.searchRepositories("2023-01-01", "java", 1, ITEMS_PER_PAGE)
        );
    }

    @Test
    @DisplayName("Should handle malformed JSON response")
    void searchRepositories_MalformedJson() {
        mockServer.expect(requestTo(containsString("language:java")))
                .andRespond(withSuccess("{ invalid_json: ", MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class, () ->
                githubClient.searchRepositories("2023-01-01", "java", 1, ITEMS_PER_PAGE)
        );
    }
}
