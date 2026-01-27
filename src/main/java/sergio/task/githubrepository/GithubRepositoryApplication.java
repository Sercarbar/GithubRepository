package sergio.task.githubrepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GithubRepositoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(GithubRepositoryApplication.class, args);
    }
}