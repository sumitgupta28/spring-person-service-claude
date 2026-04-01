---
name: solid-principles
description: S.O.L.I.D. principles with Java/Spring Boot examples. Use when reviewing code for extensibility, when the user asks about SOLID, or when refactoring rigid or fragile code.
---

# SOLID Principles Skill

SOLID applied to Java and Spring Boot with concrete examples and violations.

## When to Use
- "apply SOLID" / "is this SOLID?"
- Reviewing code for extensibility
- Refactoring a class that's grown too large or has too many responsibilities

---

## S — Single Responsibility Principle

> A class should have one reason to change.

```java
// ❌ Violates SRP — UserService does too much
@Service
public class UserService {
    public User createUser(UserRequest req) { }
    public void sendWelcomeEmail(User user) { }  // Email sending — different responsibility
    public byte[] exportUsersCsv() { }           // CSV export — different responsibility
    public boolean validatePassword(String pw) { } // Validation — different responsibility
}

// ✅ Split responsibilities
@Service
public class UserService {
    public User createUser(UserRequest req) { }
    public User findById(Long id) { }
}

@Service
public class UserEmailService {
    public void sendWelcomeEmail(User user) { }
}

@Component
public class UserCsvExporter {
    public byte[] export(List<User> users) { }
}
```

**Signals of SRP violation:**
- Class name contains "And" or "Manager"
- Class has more than ~5-7 public methods with unrelated concerns
- Multiple reasons the class might change (business rules AND UI AND persistence)

---

## O — Open/Closed Principle

> Open for extension, closed for modification.

```java
// ❌ Violates OCP — must modify this class to add new discount type
public class DiscountCalculator {
    public BigDecimal calculate(Order order, String type) {
        return switch (type) {
            case "GOLD" -> order.getTotal().multiply(new BigDecimal("0.8"));
            case "SILVER" -> order.getTotal().multiply(new BigDecimal("0.9"));
            // Adding PLATINUM requires modifying this class
            default -> order.getTotal();
        };
    }
}

// ✅ Extend without modifying
public interface DiscountStrategy {
    BigDecimal apply(Order order);
    String getType();
}

@Component
public class GoldDiscount implements DiscountStrategy {
    public BigDecimal apply(Order order) { return order.getTotal().multiply(new BigDecimal("0.8")); }
    public String getType() { return "GOLD"; }
}

// Spring-managed — add PLATINUM by adding a new class, touching nothing else
@Component
public class DiscountCalculator {
    private final Map<String, DiscountStrategy> strategies;

    public DiscountCalculator(List<DiscountStrategy> strategies) {
        this.strategies = strategies.stream()
            .collect(Collectors.toMap(DiscountStrategy::getType, Function.identity()));
    }

    public BigDecimal calculate(Order order, String type) {
        return strategies.getOrDefault(type, o -> o.getTotal()).apply(order);
    }
}
```

---

## L — Liskov Substitution Principle

> Subtypes must be substitutable for their base types without breaking correctness.

```java
// ❌ Violates LSP — subtype throws where base type does not
public class Rectangle {
    protected int width, height;
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) { this.width = this.height = w; }  // Breaks Rectangle contract
    @Override
    public void setHeight(int h) { this.width = this.height = h; }
}

// Code using Rectangle breaks when given a Square:
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(3);
r.area();  // 9, not 15 — LSP violated

// ✅ Don't inherit — use separate types or composition
public interface Shape { int area(); }
public record Rectangle(int width, int height) implements Shape { public int area() { return width * height; } }
public record Square(int side) implements Shape { public int area() { return side * side; } }
```

**Spring Boot signals of LSP violation:**
- Override throws `UnsupportedOperationException`
- Override weakens postconditions (returns null where base returns non-null)
- Override ignores parameters accepted by base method

---

## I — Interface Segregation Principle

> Clients should not be forced to depend on methods they don't use.

```java
// ❌ Fat interface forces implementors to stub unused methods
public interface UserRepository {
    User findById(Long id);
    List<User> findAll();
    void save(User user);
    void delete(Long id);
    List<User> findByRole(String role);
    Page<User> findAllPaged(Pageable pageable);
    long count();
    void bulkImport(List<User> users);  // Only used by admin batch job
}

// ✅ Segregated interfaces (Spring Data does this naturally)
public interface UserReader {
    User findById(Long id);
    Page<User> findAll(Pageable pageable);
}

public interface UserWriter {
    User save(User user);
    void delete(Long id);
}

public interface UserBulkOperations {
    void bulkImport(List<User> users);
}

// Services depend only on what they need
@Service
public class UserQueryService {
    private final UserReader userReader;  // Not forced to depend on write operations
}
```

---

## D — Dependency Inversion Principle

> High-level modules should not depend on low-level modules. Both should depend on abstractions.

```java
// ❌ High-level service depends directly on low-level implementation
@Service
public class OrderService {
    private final MySqlOrderRepository repository;  // Concrete class — hard to test, hard to swap
}

// ✅ Depend on abstraction
@Service
public class OrderService {
    private final OrderRepository repository;  // Interface — injected by Spring

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

public interface OrderRepository extends JpaRepository<Order, Long> { }
```

```java
// ❌ Service creates its own dependencies
@Service
public class NotificationService {
    private final EmailClient emailClient = new SmtpEmailClient();  // Hard dependency

    public void notify(User user) {
        emailClient.send(user.getEmail(), "Hello!");
    }
}

// ✅ Inject abstraction (easy to swap implementation or mock in tests)
@Service
public class NotificationService {
    private final EmailClient emailClient;

    public NotificationService(EmailClient emailClient) {
        this.emailClient = emailClient;
    }
}
```

---

## Quick Reference

| Principle | Violation Signal | Fix |
|-----------|-----------------|-----|
| SRP | Class doing unrelated things, name has "And" | Extract into focused classes |
| OCP | Switch/if-else on type to select behaviour | Strategy pattern + polymorphism |
| LSP | Subclass throws or ignores inherited behaviour | Composition over inheritance |
| ISP | Implementing interface with `throw new UnsupportedOperationException()` | Split interface |
| DIP | `new ConcreteImpl()` inside a service | Constructor injection of interface |
