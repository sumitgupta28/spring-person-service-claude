---
name: java-code-review
description: Systematic Java code review checklist covering correctness, concurrency, API design, and maintainability. Use when asked to review Java classes, methods, or PRs at the language level.
---

# Java Code Review Skill

Systematic checklist for reviewing Java code quality, correctness, and safety.

## When to Use
- "review this Java class / method / PR"
- Checking code before merge
- Ensuring Java best practices are followed

---

## Review Checklist

### 1. Correctness

```java
// ❌ Comparing strings with ==
if (status == "ACTIVE") { }  // Reference equality, not value

// ✅
if ("ACTIVE".equals(status)) { }  // Safe even if status is null
if (status != null && status.equals("ACTIVE")) { }
```

```java
// ❌ Integer cache trap
Integer a = 128, b = 128;
a == b  // false (outside cache range -128..127)

// ✅
a.equals(b)
```

```java
// ❌ Mutable static field
public static List<String> ALLOWED_ROLES = new ArrayList<>();  // Shared mutable state

// ✅
public static final List<String> ALLOWED_ROLES = List.of("ADMIN", "USER");
```

### 2. Null Safety

```java
// ❌ NPE chain
String city = user.getAddress().getCity().toUpperCase();

// ✅ Optional chain
String city = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .map(String::toUpperCase)
    .orElse("Unknown");
```

**Flags:**
- `Optional.get()` without `isPresent()`
- Methods returning `null` instead of `Optional<T>` or empty collection
- Missing null checks on method parameters in public APIs
- `@NotNull` / `@NonNull` absent on public method signatures

### 3. Collections & Streams

```java
// ❌ Modifying collection during iteration
for (User u : users) {
    if (!u.isActive()) users.remove(u);  // ConcurrentModificationException
}

// ✅
users.removeIf(u -> !u.isActive());
```

```java
// ❌ Collecting to then stream again
List<String> names = users.stream()
    .map(User::getName)
    .collect(toList());  // Intermediate list not needed
names.stream().forEach(System.out::println);

// ✅ Single pipeline
users.stream()
    .map(User::getName)
    .forEach(System.out::println);
```

```java
// ❌ Returning internal mutable collection
public List<String> getRoles() { return this.roles; }  // Caller can mutate

// ✅ Defensive copy
public List<String> getRoles() { return List.copyOf(this.roles); }
```

### 4. equals / hashCode

```java
// ❌ Overrides equals but not hashCode
@Override
public boolean equals(Object o) { ... }
// Missing hashCode → broken HashMap/HashSet behavior

// ✅ Always override both
@Override
public boolean equals(Object o) { ... }

@Override
public int hashCode() { return Objects.hash(id, email); }
```

### 5. Exception Handling

```java
// ❌ Too broad catch
try { process(); } catch (Exception e) { log.error("Error", e); }
// Catches InterruptedException, OutOfMemoryError, etc.

// ✅ Specific exceptions
try { process(); }
catch (IOException e) { log.error("IO error processing {}", filename, e); throw new ProcessingException(e); }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // Restore interrupt flag
    throw new ProcessingException("Interrupted", e);
}
```

```java
// ❌ Checked exception in lambda
list.forEach(item -> {
    process(item);  // If process() throws checked exception → won't compile
});

// ✅ Wrap or use unchecked
list.forEach(item -> {
    try { process(item); }
    catch (IOException e) { throw new UncheckedIOException(e); }
});
```

### 6. Resource Management

```java
// ❌ Manual close (skipped on exception)
Connection conn = dataSource.getConnection();
conn.close();

// ✅ Try-with-resources
try (Connection conn = dataSource.getConnection()) {
    // use conn
}
```

### 7. Immutability & Thread Safety

```java
// ❌ Mutable fields exposed
public class Config {
    public List<String> hosts = new ArrayList<>();
}

// ✅ Immutable value object
public record Config(List<String> hosts) {
    public Config { hosts = List.copyOf(hosts); }  // Defensive copy in compact constructor
}
```

---

## Review Output Format

```markdown
### Critical
- **Correctness** (OrderService.java:34): String compared with `==`. Use `.equals()`.

### Important
- **Null safety** (UserController.java:58): `user.getEmail().toLowerCase()` — NPE if email null.
- **hashCode missing** (Product.java:12): `equals()` overridden without `hashCode()`.

### Minor
- **Mutable return** (RoleService.java:89): Returns internal list. Use `List.copyOf()`.

### Good
- ✅ Constructor injection throughout
- ✅ Try-with-resources on all DB connections
```
