package sergio.task.githubrepository.external;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class GithubClientResilienceTest {

    @Autowired
    private GithubClient githubClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private RestClient restClient;

    @Test
    @DisplayName("Circuit Breaker - Should transition to OPEN when GitHub API calls fail")
    void shouldOpenCircuitBreakerOnFailure() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("githubSearch");
        breaker.reset();

        when(restClient.get()).thenThrow(new RuntimeException("Network error"));

        for (int i = 0; i < 10; i++) {
            try {
                githubClient.searchRepositories("2023-01-01", "java", 1, 100);
            } catch (Exception ignored) {
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
    }
}