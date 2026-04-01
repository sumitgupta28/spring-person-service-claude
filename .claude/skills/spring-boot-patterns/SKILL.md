---
name: spring-boot-patterns
description: Spring Boot best practices — configuration, profiles, exception handling, events, caching, scheduling. Use when building Spring Boot features or reviewing Spring-specific code.
---

# Spring Boot Patterns Skill

Production-ready Spring Boot patterns beyond the basics.

## When to Use
- "how should I configure this in Spring Boot"
- "what's the Spring Boot way to do X"
- Reviewing Spring-specific implementation choices

---

## Configuration Properties

```java
// ❌ @Value everywhere — scattered, hard to validate
@Value("${payment.api.url}")
private String paymentUrl;
@Value("${payment.api.key}")
private String apiKey;
@Value("${payment.api.timeout:5000}")
private int timeout;

// ✅ @ConfigurationProperties — typed, validated, autocompleted
@ConfigurationProperties(prefix = "payment.api")
@Validated
public record PaymentApiProperties(
    @NotBlank String url,
    @NotBlank String key,
    @Min(100) @Max(30000) int timeout
) {
    public PaymentApiProperties {
        timeout = timeout == 0 ? 5000 : timeout;  // Default in compact constructor
    }
}

// Register
@SpringBootApplication
@ConfigurationPropertiesScan
public class App { }
```

```yaml
# application.yml
payment:
  api:
    url: https://api.payment.com
    key: ${PAYMENT_API_KEY}  # From environment
    timeout: 5000
```

---

## Profiles

```java
// ✅ Profile-specific beans
@Service
@Profile("!prod")
public class StubEmailService implements EmailService {
    public void send(String to, String subject, String body) {
        log.info("STUB email to {}: {}", to, subject);
    }
}

@Service
@Profile("prod")
public class SmtpEmailService implements EmailService {
    // Real implementation
}
```

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
  h2:
    console.enabled: true

# application-prod.yml
spring:
  jpa:
    show-sql: false
logging:
  level:
    root: WARN
    com.example: INFO
```

---

## Exception Handling

```java
// ✅ Global handler with structured error response
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid"
            ));
        return new ValidationErrorResponse("VALIDATION_FAILED", errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
    }
}

public record ErrorResponse(String code, String message) {}
public record ValidationErrorResponse(String code, Map<String, String> fields) {}
```

---

## Spring Events

```java
// ✅ Decouple side effects with events
public record UserRegisteredEvent(User user) {}

@Service
public class UserService {
    private final ApplicationEventPublisher events;

    @Transactional
    public User register(UserRequest req) {
        User user = createUser(req);
        events.publishEvent(new UserRegisteredEvent(user));  // Sync by default
        return user;
    }
}

@Component
public class WelcomeEmailListener {
    @EventListener
    @Async  // Async — doesn't delay the HTTP response
    public void onUserRegistered(UserRegisteredEvent event) {
        emailService.sendWelcome(event.user());
    }
}

@Component
public class AuditListener {
    @TransactionalEventListener(phase = AFTER_COMMIT)  // Only fires if tx committed
    public void onUserRegistered(UserRegisteredEvent event) {
        auditService.log("USER_REGISTERED", event.user().getId());
    }
}
```

---

## Caching

```java
// ✅ Cache configuration
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        var config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues();
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

// ✅ Cache annotations
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @CachePut(value = "products", key = "#result.id")
    @Transactional
    public Product update(Long id, ProductRequest req) {
        // Updates cache after write
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
```

---

## Scheduling

```java
@Configuration
@EnableScheduling
public class SchedulingConfig { }

@Component
public class CleanupJob {

    // Fixed rate — every 5 minutes regardless of execution time
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void cleanExpiredSessions() { }

    // Fixed delay — 10 minutes after previous execution completes
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void processRetryQueue() { }

    // Cron — every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() { }
}
```

---

## Actuator & Health

```java
// ✅ Custom health indicator
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {
    private final PaymentGatewayClient client;

    @Override
    public Health health() {
        try {
            client.ping();
            return Health.up().withDetail("gateway", "reachable").build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true  # /actuator/health/liveness + /actuator/health/readiness
```

---

## Common Anti-Patterns

| Anti-Pattern | Better Approach |
|--------------|-----------------|
| `@Autowired` on fields | Constructor injection |
| `@Value` for every property | `@ConfigurationProperties` |
| Business logic in `@Configuration` | `@Profile` beans or `@ConditionalOnProperty` |
| `@Transactional` on `@Controller` | Transactions belong in `@Service` |
| Catching all exceptions in service | Let `@RestControllerAdvice` handle |
| `@SpringBootTest` for every test | Use slices: `@WebMvcTest`, `@DataJpaTest` |
