package zm.agriswift.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String ERROR_TYPE_BASE = "https://docs.agriswift.zm/errors/";

  // ---- Domain Exceptions ----

  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
    return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found", request);
  }

  @ExceptionHandler(DomainException.class)
  public ProblemDetail handleDomain(DomainException ex, HttpServletRequest request) {
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "domain-error", request);
  }

  // ---- Persistence & Concurrency Exceptions ----

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Database constraint violation: {}", ex.getMostSpecificCause().getMessage());
    return buildProblemDetail(
            HttpStatus.CONFLICT,
            "A resource with the specified unique identifier already exists.",
            "data-integrity-violation",
            request
    );
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
    return buildProblemDetail(
            HttpStatus.CONFLICT,
            "The resource was modified concurrently. Please refresh and retry.",
            "concurrent-modification",
            request
    );
  }

  // ---- Request Parsing & Validation Exceptions ----

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail pd = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "validation-error", request);

    Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                    FieldError::getField,
                    fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("invalid value"),
                    (msg1, msg2) -> msg1 + "; " + msg2
            ));

    pd.setProperty("errors", fieldErrors);
    return pd;
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ProblemDetail handleMalformedInput(Exception ex, HttpServletRequest request) {
    return buildProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Malformed request payload or parameter type mismatch.",
            "malformed-input",
            request
    );
  }
// ---- Custom & Domain Security Exceptions ----

  @ExceptionHandler(UnauthorizedException.class)
  public ProblemDetail handleCustomUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
    return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), "unauthorized", request);
  }
  // ---- Security Exceptions ----

  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
    return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication required.", "unauthorized", request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    return buildProblemDetail(HttpStatus.FORBIDDEN, "Insufficient permissions to perform this action.", "forbidden", request);
  }

  // ---- Catch-All 500 Handler ----

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
    String traceId = resolveTraceId();
    log.error("Unexpected internal error [traceId={}]", traceId, ex);
    return buildProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal error occurred",
            "internal-error",
            request
    );
  }

  // ---- Builder Helpers ----

  private ProblemDetail buildProblemDetail(HttpStatusCode status, String detail, String errorCode, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create(ERROR_TYPE_BASE + errorCode));
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty("timestamp", Instant.now());
    pd.setProperty("traceId", resolveTraceId());
    return pd;
  }

  private String resolveTraceId() {
    return Optional.ofNullable(MDC.get("traceId"))
            .orElseGet(() -> UUID.randomUUID().toString());
  }
}