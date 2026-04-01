---
name: performance-smell-detection
description: Code-level Java performance smells — streams, boxing, regex, string concatenation, collections. Use when reviewing code for performance issues or investigating slow endpoints.
---

# Performance Smell Detection Skill

Identify common code-level performance issues in Java before they hit production.

## When to Use
- "this endpoint is slow" / "find performance issues"
- "review for performance"
- Before profiling: eliminate obvious smells first

---

## Stream & Collection Smells

### Collecting to Filter / Map

```java
// ❌ Materializes intermediate list unnecessarily
List<String> names = users.stream()
    .collect(toList());
names.stream()
    .filter(n -> n.startsWith("A"))
    .forEach(System.out::println);

// ✅ Single pipeline
users.stream()
    .map(User::getName)
    .filter(n -> n.startsWith("A"))
    .forEach(System.out::println);
```

### Stream in a Loop

```java
// ❌ Creates a new stream every iteration — O(n²) behavior
for (String id : ids) {
    orders.stream()
        .filter(o -> o.getId().equals(id))  // Full scan each time
        .findFirst()
        .ifPresent(this::process);
}

// ✅ Index first, then lookup O(1)
Map<String, Order> orderIndex = orders.stream()
    .collect(toMap(Order::getId, Function.identity()));
ids.forEach(id -> Optional.ofNullable(orderIndex.get(id))
    .ifPresent(this::process));
```

### Parallel Stream Misuse

```java
// ❌ Parallel stream for small lists (overhead > benefit)
smallList.parallelStream().map(this::cheapOp).collect(toList());

// ❌ Parallel stream with shared mutable state
List<String> results = new ArrayList<>();
list.parallelStream().forEach(results::add);  // Race condition

// ✅ Parallel only for large data + CPU-bound + no shared state
largeList.parallelStream()
    .map(this::expensiveCpuOp)
    .collect(toList());
```

---

## Boxing & Unboxing

```java
// ❌ Repeated autoboxing in hot path
Long sum = 0L;  // Boxed Long
for (int i = 0; i < 1_000_000; i++) {
    sum += i;  // Unbox Long, add int, re-box Long — 1M allocations
}

// ✅ Primitive
long sum = 0L;
for (int i = 0; i < 1_000_000; i++) {
    sum += i;
}
```

```java
// ❌ Stream boxing
OptionalDouble avg = ids.stream()  // Stream<Integer>
    .mapToInt(Integer::intValue)   // Redundant unbox
    .average();

// ✅ Use primitive streams
IntStream.of(ids).average();
```

**Flags:**
- `Integer`, `Long`, `Double` in arithmetic loops
- `Stream<Integer>` where `IntStream` would work
- `Map<Long, X>` key comparisons (use primitive where possible)

---

## String Concatenation

```java
// ❌ String building in loop — O(n²) allocations
String result = "";
for (String part : parts) {
    result += part + ", ";  // New String object each iteration
}

// ✅ StringBuilder
StringBuilder sb = new StringBuilder();
for (String part : parts) {
    sb.append(part).append(", ");
}
String result = sb.toString();

// ✅ Or String.join
String result = String.join(", ", parts);
```

```java
// ❌ Logging with concatenation (evaluated even if level disabled)
log.debug("Processing user " + userId + " with role " + role);

// ✅ Parameterized (lazy evaluation)
log.debug("Processing user {} with role {}", userId, role);
```

---

## Regex Compilation

```java
// ❌ Compiles pattern on every call
public boolean isValidEmail(String email) {
    return email.matches("[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+");  // Recompiles every time
}

// ✅ Compile once as constant
private static final Pattern EMAIL_PATTERN =
    Pattern.compile("[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+");

public boolean isValidEmail(String email) {
    return EMAIL_PATTERN.matcher(email).matches();
}
```

---

## I/O & Database

```java
// ❌ Fetching entire table to count
long count = userRepository.findAll().size();  // Loads all rows

// ✅ Count query
long count = userRepository.count();
```

```java
// ❌ Loading full entities for a projection
List<User> users = userRepository.findAll();
List<String> emails = users.stream().map(User::getEmail).collect(toList());

// ✅ Projection query
@Query("SELECT u.email FROM User u")
List<String> findAllEmails();
```

```java
// ❌ No pagination on potentially large results
@GetMapping("/orders")
public List<Order> getAllOrders() {
    return orderRepository.findAll();  // Could be millions
}

// ✅ Paginated
@GetMapping("/orders")
public Page<Order> getOrders(@PageableDefault(size = 20) Pageable pageable) {
    return orderRepository.findAll(pageable);
}
```

---

## Caching

```java
// ❌ No caching for expensive read-only computation
public List<Permission> getPermissions(String role) {
    return permissionRepository.findByRole(role);  // DB hit every call
}

// ✅ Cache with Spring @Cacheable
@Cacheable("permissions")
public List<Permission> getPermissions(String role) {
    return permissionRepository.findByRole(role);
}
```

---

## Quick-Reference Flags

| Smell | Code Signal |
|-------|-------------|
| Stream in loop | `stream()` inside `for`/`while` |
| Unnecessary collect | `.collect(toList()).stream()` |
| Boxed arithmetic | `Long`/`Integer` in `+=` loops |
| Pattern recompile | `String.matches()` or `Pattern.compile()` in method body |
| String concat in loop | `result += ...` inside loop |
| Full table load | `findAll()` followed by `.size()` or `.stream().filter()` |
| Missing pagination | `List<X>` return on collection endpoints |
| Eager fetch | `FetchType.EAGER` on `@OneToMany` |
