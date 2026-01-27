# GitHub Repository Popularity Scorer - Backend Challenge

## Project Overview

This project is a high-performance backend application designed to search and score GitHub repositories based on their
popularity. The objective is to provide a ranked list of repositories using a custom scoring algorithm that considers
stars, forks, and the recency of updates.

The application is built with **Java 21**, leveraging **Virtual Threads** to achieve high scalability and performance
while maintaining clean and concise code.

## Key Features

* **Dynamic Search**: Allows users to configure the earliest created date and the programming language.
* **Popularity Scoring**: A custom algorithm assigns a score based on stars, forks, and update recency.
* **High Scalability**: Utilizes Java 21 Virtual Threads to handle concurrent API calls to GitHub's public search
  endpoint.
* **Intelligent Caching**: Implements a caching layer to drastically reduce latency
* **Resilience**: Integrated **Circuit Breaker** (Resilience4j) to handle external API failures and rate limits
  gracefully.
* **Clean Architecture**: Separation of concerns between external, service, and domain layers.

## Tech Stack

* **Language**: Java 21 (Required for Virtual Threads).
* **Framework**: Spring Boot 3.x.
* **HTTP Client**: `RestClient` (Synchronous implementation executed via Virtual Threads for non-blocking behavior).
* **Caching**: Spring Cache (@Cacheable) with a composite key (createdAfter + language)
* **Resilience**: Resilience4j (Circuit Breaker).
* **Testing**: JUnit 5, Mockito.

## Scoring Algorithm

The algorithm uses a non-linear approach using to balance massive repositories with trending ones, avoiding bias toward
extreme outliers.

$$Score = ( \log_{10}(Stars + 1) \times W_{stars} + \log_{10}(Forks + 1) \times W_{forks} ) \times Multiplier_{freshness}$$

* **Stars**: Direct measure of community interest.
* **Forks**: Weighted more heavily as it represents active contribution.
* **Multiplier of freshness**: Function that adjusts the score based on how recently the repository was updated
  $$Multiplier_{freshness} = \begin{cases}
  1.5 & \text{if } days \leq 3 \text{ (Very Recent)} \\
  1.2 & \text{if } days \leq 14 \text{ (Recent)} \\
  0.5 & \text{if } days > 365 \text{ (Old)} \\
  1.0 & \text{otherwise (Default)}
  \end{cases}$$

    * **Boost Very Recent (1.5x)**: Significant score increase for repositories updated within the last 3 days.
    * **Boost Recent (1.2x)**: Moderate increase for activity within the last 14 days.
    * **Penalty Old (0.5x)**: 50% score reduction for repositories that haven't seen activity in over a year (365 days).
    * **Default (1.0x)**: No adjustment for repositories updated between 15 and 365 days ago.

### 1. Logarithmic Base Score

The use of $\log_{10}$ for **Stars** and **Forks** ensures that extremely popular repositories do not completely
overshadow smaller, highly relevant ones. This provides a more balanced ranking.

## Scalability & Design Decisions

As per the challenge guidelines, implementing software is about trade-offs. I have prioritized:

1. **Massive Concurrency with Virtual Threads (Java 21):** I chose Virtual Threads over a fully Reactive stack (WebFlux)
   to maintain an imperative and readable codebase. This technology enables **Parallel Fetching** via a
   `VirtualThreadPerTaskExecutor`, allowing the app to scale to thousands of concurrent requests and significantly
   reducing response times without the complexity of reactive streams.
2. **Intelligent Caching Strategy (Caffeine):** To maximize performance and respect GitHub's API limits, I implemented a
   caching layer with a composite key (`createdAfter` + `language`). This prevents redundant external calls, ensures
   near-instant responses for frequent queries, and protects our API quota.
3. **Circuit Breaker Pattern:** Utilizes **Resilience4j** to protect the application from cascading failures. If the
   GitHub API is down or the rate limit is reached, the system responds via a fallback mechanism, maintaining overall
   service stability.

## Production-Ready Improvements

To transition this project into a real-world production environment, the following improvements are recommended:

* **Distributed Caching**: Replace the current in-memory cache with **Redis** to ensure data consistency across multiple
  service instances.
* **Proactive Rate Limiting (Token Bucket)**: Implement a **Token Bucket algorithm** (using libraries like **Bucket4j**)
  to manage GitHub API quotas. This allows the system to handle burst traffic while ensuring it stays within the
  provider's legal limits, avoiding 403 Forbidden errors.
* **Observability**: Integrate **Spring Boot Actuator**, **Prometheus**, and **Grafana** for real-time monitoring of
  performance and scalability.
* **Database Persistence (e.g., PostgreSQL):** To store historical snapshots of repository scores, enabling long-term
  trend analysis and providing a persistent data fallback that reduces reliance on the external GitHub API.

> **Note on Proactive Rate Limiting:** While professional rate-limiting (e.g., Token Bucket) is noted as a production
> improvement, it was intentionally omitted in this version to avoid over-engineering, relying instead on **Intelligent
Caching** and **Circuit Breaker** to manage API quotas effectively within the challenge's scope.

## Getting Started

### Prerequisites

* **Java 21** & **Maven 3.9+**
* **GitHub Personal Access Token (PAT)**: Highly recommended to avoid `403 Forbidden` errors due to strict rate limits
  on unauthenticated requests.

### Configuration

The application is configured via `src/main/resources/application.yml`. You can override settings using environment
variables:

```yaml
app:
  github:
    token: ${GITHUB_TOKEN}              
    max-pages-to-fetch: 5               
  scoring:
    freshness:
      very-recent-days: 3
      recent-days: 14
      old-days: 365
      boost-very-recent: 1.5
      boost-recent: 1.2
      penalty-old: 0.5
      default-multiplier: 1.0
```

### Manual Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Sercarbar/GithubRepository.git
    ```
2. **Build the project:**
    ```bash
   ./mvnw clean install
    ```
3. **Run the application:**
   ```bash
   java -jar target/GithubRepository-0.0.1-SNAPSHOT.jar --app.github.token=YOUR_TOKEN_HERE
   ```
   or
   ```bash
   ./mvnw spring-boot:run "-Dspring-boot.run.arguments=--app.github.token=YOUR_TOKEN_HERE"
   ```

## API Usage & Documentation

Once the application is running, you can explore and test the API through the following entry points:

### Interactive Documentation (Swagger UI)
The project includes **Swagger UI**, which allows you to visualize and interact with the API’s resources without having any of the implementation logic in place.
* **URL:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Usage:** Use the "Try it out" button to execute requests directly from your browser.

### Health & Monitoring (Spring Actuator)
Basic health monitoring is available to check the status of the service and its dependencies:
* **Status Endpoint:** [http://localhost:8080/health](http://localhost:8080/health)

### Primary Endpoint
**`GET /v1/repositories/popular`**

Retrieves a ranked list of GitHub repositories based on the calculated popularity score.

### Query Parameters:
| Parameter  | Type     | Required | Description                                           |
| :--------- | :------- | :------- | :---------------------------------------------------- |
| `since`    | `String` | Yes      | Earliest creation date (Format: `dd-MM-yyyy`).        |
| `language` | `String` | No       | Programming language to filter (e.g., `java`, `python`). |

#### Example Request (cURL):
```bash
curl -X GET "http://localhost:8080/v1/repositories/popular?since=01-01-2023&language=java"