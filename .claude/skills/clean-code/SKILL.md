---
name: clean-code
description: DRY, KISS, YAGNI, naming, and refactoring for Java. Use when refactoring messy code, reviewing for readability, or applying clean code principles.
---

# Clean Code Skill

DRY, KISS, YAGNI, naming, and practical refactoring for Java/Spring Boot.

## When to Use
- "clean up this code" / "refactor for readability"
- "apply clean code" / "reduce duplication"
- Reviewing methods that are too long or hard to follow

---

## DRY — Don't Repeat Yourself

```java
// ❌ Duplicated validation scattered across methods
public void createUser(UserRequest req) {
    if (req.getEmail() == null || !req.getEmail().contains("@")) {
        throw new ValidationException("Invalid email");
    }
    // ...
}

public void updateUser(Long id, UserRequest req) {
    if (req.getEmail() == null || !req.getEmail().contains("@")) {
        throw new ValidationException("Invalid email");
    }
    // ...
}

// ✅ Single source of truth
// Option 1: Bean Validation on the DTO (preferred)
public record UserRequest(@NotBlank @Email String email) {}

// Option 2: Private helper
private void validateEmail(String email) {
    if (email == null || !email.contains("@")) {
        throw new ValidationException("Invalid email");
    }
}
```

---

## KISS — Keep It Simple

```java
// ❌ Over-engineered for a straightforward operation
public class UserStatusEvaluatorStrategyFactory {
    public UserStatusEvaluatorStrategy getEvaluator(UserType type) {
        return switch (type) {
            case ADMIN -> new AdminStatusEvaluatorStrategy();
            case GUEST -> new GuestStatusEvaluatorStrategy();
        };
    }
}

// ✅ Simple method is enough
public boolean isActive(User user) {
    return user.getStatus() == UserStatus.ACTIVE && !user.isLocked();
}
```

```java
// ❌ Complex stream pipeline that's hard to read
List<String> result = users.stream()
    .filter(u -> u.getOrders().stream().anyMatch(o -> o.getTotal().compareTo(THRESHOLD) > 0))
    .flatMap(u -> u.getRoles().stream())
    .distinct()
    .sorted()
    .collect(toList());

// ✅ Extract intermediate steps with meaningful names
List<User> highValueUsers = users.stream()
    .filter(this::hasHighValueOrder)
    .collect(toList());

List<String> roles = highValueUsers.stream()
    .flatMap(u -> u.getRoles().stream())
    .distinct()
    .sorted()
    .collect(toList());

private boolean hasHighValueOrder(User user) {
    return user.getOrders().stream()
        .anyMatch(o -> o.getTotal().compareTo(THRESHOLD) > 0);
}
```

---

## YAGNI — You Aren't Gonna Need It

```java
// ❌ Premature abstractions for hypothetical future needs
public interface UserServiceV1 { }
public interface UserServiceV2 { }
public abstract class AbstractUserServiceBase { }
public class ConfigurableUserServiceProviderFactory { }

// ✅ Implement what you need now
@Service
public class UserService {
    public User findById(Long id) { ... }
    public User create(UserRequest req) { ... }
}
```

```java
// ❌ Unused constructor parameters "for future flexibility"
public UserService(
    UserRepository repo,
    EmailService emailService,
    AuditService auditService,   // Not used yet
    MetricsService metricsService  // Not used yet
) { }

// ✅ Inject only what you use today
public UserService(UserRepository repo, EmailService emailService) { }
```

---

## Naming

### Methods — verb phrases describing what they do

```java
// ❌ Vague
public List<User> get(String s) { }
public boolean check(User u) { }
public void doProcess() { }

// ✅ Intention-revealing
public List<User> findActiveUsersByRole(String role) { }
public boolean isEligibleForDiscount(User user) { }
public void sendWelcomeEmail(User user) { }
```

### Variables — noun phrases, no abbreviations

```java
// ❌
int d;
String usr;
List<User> ul;
boolean flg;

// ✅
int daysSinceLastLogin;
String username;
List<User> activeUsers;
boolean isEmailVerified;
```

### Classes — single noun, no "Manager" / "Util" / "Helper" (usually)

```java
// ❌ Meaningless umbrella names
public class UserManager { }
public class OrderUtils { }
public class ServiceHelper { }

// ✅ Names that reflect responsibility
public class UserRegistrationService { }
public class OrderPriceCalculator { }
public class EmailTemplateRenderer { }
```

---

## Method Length & Complexity

**Rule of thumb:** A method should fit on one screen (~20 lines). Each method does one thing.

```java
// ❌ Long method mixing concerns
@Transactional
public Order processOrder(OrderRequest request) {
    // 1. Validate
    if (request.getItems() == null || request.getItems().isEmpty()) {
        throw new ValidationException("No items");
    }
    // 2. Calculate price
    BigDecimal total = BigDecimal.ZERO;
    for (OrderItem item : request.getItems()) {
        Product p = productRepository.findById(item.getProductId()).orElseThrow();
        total = total.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
    // 3. Apply discount
    if (request.getCouponCode() != null) {
        Coupon coupon = couponRepository.findByCode(request.getCouponCode()).orElseThrow();
        total = total.multiply(BigDecimal.ONE.subtract(coupon.getDiscount()));
    }
    // 4. Save
    Order order = new Order(request.getUserId(), total);
    return orderRepository.save(order);
}

// ✅ Extracted steps with names that document intent
@Transactional
public Order processOrder(OrderRequest request) {
    validateOrderItems(request.getItems());
    BigDecimal total = calculateTotal(request.getItems());
    BigDecimal discounted = applyDiscount(total, request.getCouponCode());
    return orderRepository.save(new Order(request.getUserId(), discounted));
}
```

---

## Boolean Expressions

```java
// ❌ Cryptic condition
if (u != null && u.getStatus() == 1 && !u.getFlag() && u.getAge() >= 18) { }

// ✅ Named predicate
private boolean isEligibleUser(User u) {
    return u != null
        && u.getStatus() == UserStatus.ACTIVE
        && !u.isBlocked()
        && u.getAge() >= MINIMUM_AGE;
}

if (isEligibleUser(user)) { }
```

---

## Magic Numbers & Strings

```java
// ❌
if (user.getAge() < 18) { }
if (order.getTotal().compareTo(new BigDecimal("500")) > 0) { }

// ✅ Named constants
private static final int MINIMUM_AGE = 18;
private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500");

if (user.getAge() < MINIMUM_AGE) { }
if (order.getTotal().compareTo(FREE_SHIPPING_THRESHOLD) > 0) { }
```
