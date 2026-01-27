package sergio.task.githubrepository.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sergio.task.githubrepository.model.RepositoryScoreResponse;
import sergio.task.githubrepository.service.PopularityService;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PopularityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PopularityService service;

    @Test
    @DisplayName("Should return 200 OK and list of repositories when parameters are valid")
    void getPopularRepositories_ValidParams() throws Exception {
        RepositoryScoreResponse mockResponse = new RepositoryScoreResponse(
                "test/repo", 100, 50, "java", 200.0, "http://url"
        );

        given(service.getPopularRepositories("2023-01-01", "java"))
                .willReturn(List.of(mockResponse));

        mockMvc.perform(get("/v1/repositories/popular")
                        .param("since", "2023-01-01")
                        .param("language", "java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("test/repo"))
                .andExpect(jsonPath("$.items[0].popularityScore").value(200.0));
    }

    @Test
    @DisplayName("Should return 200 OK and empty list when no repositories match criteria")
    void getPopularRepositories_EmptyResult() throws Exception {
        given(service.getPopularRepositories("2023-01-01", "cobol"))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/repositories/popular")
                        .param("since", "2023-01-01")
                        .param("language", "cobol"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when language parameter is missing")
    void getPopularRepositories_MissingLanguage() throws Exception {
        mockMvc.perform(get("/v1/repositories/popular")
                        .param("since", "2023-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when date format is invalid")
    void getPopularRepositories_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/v1/repositories/popular")
                        .param("since", "not-a-date")
                        .param("language", "java"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Data"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Invalid value ('not-a-date')")));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when service fails")
    void getPopularRepositories_ServiceError() throws Exception {
        given(service.getPopularRepositories("2023-01-01", "java"))
                .willThrow(new RuntimeException("GitHub API is down"));

        mockMvc.perform(get("/v1/repositories/popular")
                        .param("since", "2023-01-01")
                        .param("language", "java"))
                .andExpect(status().isInternalServerError());
    }
}