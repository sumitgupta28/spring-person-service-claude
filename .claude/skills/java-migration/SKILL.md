---
name: java-migration
description: Java version upgrade guide (8→11→17→21) — language features, removed APIs, and migration steps. Use when upgrading Java versions or adopting newer language features.
---

# Java Migration Skill

Upgrade guide and feature adoption for Java 8 → 11 → 17 → 21.

## When to Use
- "upgrade from Java 8 to 17" / "migrate to Java 21"
- "what changed in Java X"
- Adopting records, sealed classes, or virtual threads

---

## Quick Feature Map

| Feature | Available Since |
|---------|----------------|
| `var` local type inference | Java 11 |
| `String::isBlank`, `strip()` | Java 11 |
| `Files.readString()`, `writeString()` | Java 11 |
| Switch expressions | Java 14 (final) |
| Records | Java 16 (final) |
| Pattern matching `instanceof` | Java 16 (final) |
| Sealed classes | Java 17 (final) |
| Text blocks | Java 15 (final) |
| Pattern matching in switch | Java 21 (final) |
| Virtual Threads | Java 21 (final) |
| Sequenced Collections | Java 21 |

---

## Java 8 → 11

### Removed APIs — fix these first

```java
// ❌ javax.* removed (moved to separate modules)
import javax.xml.bind.JAXBContext;  // Removed from JDK 11

// ✅ Add explicit dependency
// Maven:
// <dependency>
//   <groupId>jakarta.xml.bind</groupId>
//   <artifactId>jakarta.xml.bind-api</artifactId>
// </dependency>
```

```java
// ❌ sun.misc.* internal APIs
import sun.misc.BASE64Encoder;

// ✅ Use java.util.Base64 (available since Java 8)
Base64.getEncoder().encodeToString(bytes);
```

### New String Methods

```java
// Java 11 additions
"  hello  ".strip()       // Unicode-aware trim (prefer over trim())
"  ".isBlank()            // true
"a\nb\nc".lines()         // Stream<String>
"abc".repeat(3)           // "abcabcabc"
```

### var (Java 10+)

```java
// ✅ Use var for obvious types
var users = new ArrayList<User>();
var map = new HashMap<String, List<Order>>();

// ❌ Don't use var when type is unclear
var result = process(data);  // What type is result?

// ✅ Use explicit type when not obvious
ProcessingResult result = process(data);
```

---

## Java 11 → 17

### Records (Java 16)

```java
// ❌ Old DTO style
public class UserResponse {
    private final Long id;
    private final String name;

    public UserResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }
    // + getters, equals, hashCode, toString boilerplate
}

// ✅ Record
public record UserResponse(Long id, String name) {}

// Custom validation in compact constructor
public record UserRequest(@NotBlank String email, @NotBlank String name) {
    public UserRequest {
        email = email.toLowerCase();  // Normalize in compact constructor
    }
}
```

### Pattern Matching instanceof (Java 16)

```java
// ❌ Old style
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}

// ✅ Pattern matching
if (obj instanceof String s) {
    System.out.println(s.length());
}
```

### Switch Expressions (Java 14)

```java
// ❌ Old switch statement
String result;
switch (status) {
    case ACTIVE: result = "Active"; break;
    case INACTIVE: result = "Inactive"; break;
    default: result = "Unknown";
}

// ✅ Switch expression
String result = switch (status) {
    case ACTIVE -> "Active";
    case INACTIVE -> "Inactive";
    default -> "Unknown";
};
```

### Text Blocks (Java 15)

```java
// ❌ String concatenation for multi-line
String json = "{\n" +
    "  \"name\": \"" + name + "\",\n" +
    "  \"email\": \"" + email + "\"\n" +
    "}";

// ✅ Text block
String json = """
    {
      "name": "%s",
      "email": "%s"
    }
    """.formatted(name, email);
```

### Sealed Classes (Java 17)

```java
// ✅ Model closed hierarchies explicitly
public sealed interface Shape permits Circle, Rectangle, Triangle {}

public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double base, double height) implements Shape {}

// Compiler ensures exhaustive handling
double area = switch (shape) {
    case Circle c -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.width() * r.height();
    case Triangle t -> 0.5 * t.base() * t.height();
    // No default needed — compiler verifies all cases covered
};
```

---

## Java 17 → 21

### Virtual Threads (Java 21)

```java
// Enable in Spring Boot 3.2+
// application.yml:
// spring.threads.virtual.enabled: true

// Or programmatically for custom executors
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// ✅ Virtual threads — cheap, create freely
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i ->
        executor.submit(() -> blockingIoOperation())
    );
}
```

### Pattern Matching in Switch (Java 21)

```java
// ✅ Type patterns in switch
String describe(Object obj) {
    return switch (obj) {
        case Integer i when i < 0 -> "negative integer: " + i;
        case Integer i -> "integer: " + i;
        case String s when s.isBlank() -> "blank string";
        case String s -> "string: " + s;
        case null -> "null";
        default -> "other: " + obj;
    };
}
```

### Sequenced Collections (Java 21)

```java
// ✅ SequencedCollection — get first/last without index tricks
SequencedCollection<String> list = new ArrayList<>(List.of("a", "b", "c"));
list.getFirst();  // "a"
list.getLast();   // "c"
list.reversed();  // View in reverse order
```

---

## Spring Boot Version Alignment

| Java Version | Minimum Spring Boot |
|--------------|---------------------|
| Java 8 | Spring Boot 2.x |
| Java 11 | Spring Boot 2.5+ |
| Java 17 | Spring Boot 2.7+ (3.x recommended) |
| Java 21 | Spring Boot 3.2+ |

**Migration path:** Java 8 + Spring Boot 2.x → Java 17 + Spring Boot 3.x → Java 21 + Spring Boot 3.2+

**Spring Boot 3.x requires:**
- Java 17 minimum
- `jakarta.*` imports (not `javax.*`) — global search-replace needed
- Spring Security 6.x syntax changes (`authorizeHttpRequests` replaces `authorizeRequests`)

---

## Migration Checklist

### To Java 17 + Spring Boot 3.x
- [ ] Replace all `javax.*` imports with `jakarta.*`
- [ ] Update `SecurityFilterChain` configuration syntax
- [ ] Replace `@Configuration(proxyBeanMethods = false)` where applicable
- [ ] Convert DTOs to records where mutable state not needed
- [ ] Replace `instanceof` casts with pattern matching

### To Java 21
- [ ] Enable virtual threads: `spring.threads.virtual.enabled=true`
- [ ] Replace `synchronized` blocks with `ReentrantLock`
- [ ] Remove custom thread pools for I/O-bound work (let virtual threads handle it)
- [ ] Adopt pattern matching switch for multi-type dispatch
