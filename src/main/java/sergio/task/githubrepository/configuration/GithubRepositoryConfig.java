package sergio.task.githubrepository.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import sergio.task.githubrepository.service.ScoringProperties;

@EnableConfigurationProperties(ScoringProperties.class)
@Configuration
public class GithubRepositoryConfig {

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder,
                                       @Value("${app.github.api-url:https://api.github.com}") String baseUrl,
                                       @Value("${app.github.token:}") String token) {


        var clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (token != null && !token.isBlank()) {
            clientBuilder.defaultHeader("Authorization", "Bearer " + token);
        }

        return clientBuilder.build();
    }
}