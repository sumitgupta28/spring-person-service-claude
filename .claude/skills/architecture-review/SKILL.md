---
name: architecture-review
description: Macro-level architecture review for Spring Boot — packages, module boundaries, layer violations, and dependency direction. Use when reviewing project structure, identifying architectural drift, or planning a refactor.
---

# Architecture Review Skill

Macro-level review of Spring Boot project structure, layer boundaries, and dependency direction.

## When to Use
- "review the architecture" / "check project structure"
- Identifying layer violations or circular dependencies
- Planning a modular monolith or microservices split
- Onboarding to a new codebase

---

## Expected Layered Structure

```
src/main/java/com/example/
├── controller/     Web layer — HTTP in, DTO out. No business logic.
├── service/        Application layer — orchestrates use cases, owns transactions.
├── repository/     Data layer — Spring Data interfaces only.
├── model/          Domain — JPA entities, domain objects.
├── dto/            Transfer objects — records for request/response.
├── config/         Spring @Configuration classes.
└── exception/      Custom exceptions + @RestControllerAdvice.
```

**Dependency direction (must flow downward only):**
```
Controller → Service → Repository → Model
```

---

## Layer Violation Detection

### Controller calling Repository directly

```java
// ❌ Layer violation — controller bypasses service
@RestController
public class UserController {
    private final UserRepository userRepository;  // Should be UserService

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
```

### Service depending on Controller types

```java
// ❌ Service importing controller-layer types
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Service
public class UserService extends ResponseEntityExceptionHandler { }  // Wrong layer
```

### Entity leaking into API response

```java
// ❌ JPA entity returned from controller (leaks persistence annotations, lazy proxy issues)
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);  // Returns @Entity, not DTO
}

// ✅ Map to DTO in service or controller
@GetMapping("/{id}")
public UserResponse getUser(@PathVariable Long id) {
    return userService.findById(id);  // Returns record UserResponse(...)
}
```

---

## Package Structure Patterns

### By Layer (default for small apps)
```
com.example.app/
├── controller/
├── service/
├── repository/
└── model/
```

### By Feature (recommended for growing apps)
```
com.example.app/
├── order/
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── OrderRepository.java
│   └── Order.java
└── user/
    ├── UserController.java
    └── ...
```

Feature packaging keeps related code co-located, makes module extraction easier, and reduces accidental cross-feature coupling.

---

## Circular Dependency Detection

```bash
# Maven
./mvnw dependency:tree | grep -E "cycle|circular"

# Gradle
./gradlew dependencies 2>&1 | grep -E "cycle|circular"

# Find circular Spring beans at startup
# Spring Boot will throw BeanCurrentlyInCreationException
```

```java
// ❌ Circular dependency
@Service
public class OrderService {
    private final PaymentService paymentService;
}

@Service
public class PaymentService {
    private final OrderService orderService;  // Cycle!
}

// ✅ Break cycle with an event or interface
// OrderService publishes OrderCreatedEvent
// PaymentService listens for OrderCreatedEvent
```

---

## Modular Monolith Readiness

Signs a module is ready to extract:
- Clear package boundary (feature package, not cross-cutting)
- No direct repository calls across feature boundaries
- Communication via service interfaces or events, not direct bean injection
- Separate database tables (no joins across feature tables)

```java
// ❌ Cross-module repository coupling (hard to extract)
@Service
public class OrderService {
    private final UserRepository userRepository;    // Order reaching into User module
    private final ProductRepository productRepository;
}

// ✅ Cross-module via service interface
@Service
public class OrderService {
    private final UserService userService;      // Depend on interface, not data layer
    private final ProductService productService;
}
```

---

## Configuration Review

```java
// ❌ Business logic in @Configuration
@Configuration
public class AppConfig {
    @Bean
    public UserService userService() {
        if (System.getenv("ENV").equals("prod")) {
            return new ProdUserService();
        }
        return new DevUserService();
    }
}

// ✅ Use @Profile or @ConditionalOnProperty
@Service
@Profile("prod")
public class ProdUserService implements UserService { }

@Service
@Profile("!prod")
public class DevUserService implements UserService { }
```

---

## Review Output Format

```markdown
## Architecture Review

### Layer Violations
- `UserController` injects `UserRepository` directly (line 12) — route through `UserService`.
- `OrderService` returns `@Entity Order` from a public method — map to `OrderResponse` DTO.

### Structural Issues
- `UserService` and `NotificationService` have a circular dependency — break with event.
- `order/` package mixes web, service, and data classes — split into sub-packages.

### Coupling Concerns
- `OrderService` imports `UserRepository` — cross-module data access should go via `UserService`.

### Good Practices Observed
- ✅ All config in `config/` package
- ✅ DTOs are Java records (immutable)
- ✅ Feature packaging used (not layer packaging)
```
