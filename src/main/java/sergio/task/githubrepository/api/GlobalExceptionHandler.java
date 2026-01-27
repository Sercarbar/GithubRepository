package sergio.task.githubrepository.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class,
            IllegalArgumentException.class
    })
    public ProblemDetail handleValidationErrors(Exception ex) {
        log.warn("Validation error: {}", ex.getMessage());

        var detail = "The request contains invalid input data.";

        switch (ex) {
            case MethodArgumentTypeMismatchException typeEx ->
                    detail = "Invalid value ('%s') for parameter '%s'. Expected type: %s."
                            .formatted(
                                    typeEx.getValue(),
                                    typeEx.getName(),
                                    (typeEx.getRequiredType() != null ? typeEx.getRequiredType().getSimpleName() : "unknown")
                            );

            case MissingServletRequestParameterException missingEx ->
                    detail = "Missing required parameter: '%s'.".formatted(missingEx.getParameterName());

            case HandlerMethodValidationException validationEx -> {
                List<String> errorMessages = new ArrayList<>();
                List<ParameterValidationResult> allResults = new ArrayList<>();

                allResults.addAll(validationEx.getValueResults());
                allResults.addAll(validationEx.getBeanResults());

                for (ParameterValidationResult res : allResults) {
                    String paramName = res.getMethodParameter().getParameterName();
                    for (var error : res.getResolvableErrors()) {
                        errorMessages.add("%s: %s".formatted(paramName, error.getDefaultMessage()));
                    }
                }
                detail = String.join(", ", errorMessages);
            }

            case IllegalArgumentException iae -> detail = iae.getMessage();

            default -> {
            }
        }

        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Request Data", detail);
    }

    @ExceptionHandler({HttpStatusCodeException.class, ResourceAccessException.class})
    public ProblemDetail handleExternalApiErrors(Exception ex) {
        log.error("External API error occurred", ex);

        if (ex instanceof HttpStatusCodeException httpEx) {
            HttpStatus status = HttpStatus.valueOf(httpEx.getStatusCode().value());
            String detail = "Error communicating with GitHub API.";

            if (status == HttpStatus.FORBIDDEN) {
                detail = "API rate limit exceeded or invalid token.";
            } else if (status == HttpStatus.NOT_FOUND) {
                detail = "Repository or resource not found on GitHub.";
            }

            return buildProblemDetail(status, "Upstream Service Error", detail);
        }

        return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Connection Error",
                "Failed to connect to GitHub. Please check your internet connection.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedErrors(Exception ex) {
        log.error("UNEXPECTED INTERNAL ERROR: ", ex);

        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please contact support.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());

        return buildProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found",
                "The requested route does not exist.");
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}