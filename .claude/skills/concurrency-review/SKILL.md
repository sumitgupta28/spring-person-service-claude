---
name: concurrency-review
description: Thread safety, race conditions, @Async, and Virtual Threads review for Java/Spring Boot. Use when reviewing shared state, async code, or multi-threaded components.
---

# Concurrency Review Skill

Identify race conditions, unsafe shared state, and misuse of async primitives in Java/Spring Boot.

## When to Use
- "review this for thread safety"
- Code uses `@Async`, `CompletableFuture`, `ExecutorService`, or `synchronized`
- Shared mutable state in Spring beans
- Migrating to Virtual Threads

---

## Spring Bean Safety

Spring beans are singletons by default. Instance fields are shared across all threads.

```java
// ❌ Mutable instance field in singleton bean
@Service
public class OrderService {
    private int orderCount = 0;  // Shared — race condition!

    public void placeOrder(Order order) {
        orderCount++;  // Not atomic
    }
}

// ✅ Use atomic types
@Service
public class OrderService {
    private final AtomicInteger orderCount = new AtomicInteger(0);

    public void placeOrder(Order order) {
        orderCount.incrementAndGet();
    }
}

// ✅ Or use stateless beans (preferred)
@Service
public class OrderService {
    // No mutable state — inherently thread-safe
    public Order placeOrder(OrderRequest request) { ... }
}
```

---

## Race Conditions

### Check-then-Act

```java
// ❌ Race condition: check and act are not atomic
if (!userRepository.existsByEmail(email)) {
    userRepository.save(new User(email));  // Another thread may insert between check and save
}

// ✅ Use database unique constraint + catch
try {
    userRepository.save(new User(email));
} catch (DataIntegrityViolationException e) {
    throw new EmailAlreadyExistsException(email);
}
```

### Lazy Initialization

```java
// ❌ Not thread-safe
private Cache cache;

public Cache getCache() {
    if (cache == null) {          // Two threads can both see null
        cache = new Cache();
    }
    return cache;
}

// ✅ Double-checked locking
private volatile Cache cache;

public Cache getCache() {
    if (cache == null) {
        synchronized (this) {
            if (cache == null) {
                cache = new Cache();
            }
        }
    }
    return cache;
}

// ✅ Or use Spring @Bean (preferred — Spring manages lifecycle)
@Bean
public Cache cache() { return new Cache(); }
```

---

## @Async Usage

```java
// ❌ Calling @Async on same class — proxy bypassed
@Service
public class EmailService {
    public void notifyUser(Long userId) {
        sendWelcomeEmail(userId);  // Direct call — @Async ignored
    }

    @Async
    public void sendWelcomeEmail(Long userId) { ... }
}

// ✅ Call @Async from a different bean
@Service
public class UserService {
    private final EmailService emailService;

    public void registerUser(UserRequest req) {
        User user = createUser(req);
        emailService.sendWelcomeEmail(user.getId());  // Goes through proxy
    }
}
```

```java
// ❌ @Async with void — exceptions are lost silently
@Async
public void processOrder(Long orderId) {
    // Any exception here vanishes
}

// ✅ Return CompletableFuture to propagate exceptions
@Async
public CompletableFuture<Void> processOrder(Long orderId) {
    try {
        // processing
        return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
    }
}

// ✅ Or configure AsyncUncaughtExceptionHandler
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async exception in {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
```

---

## CompletableFuture

```java
// ❌ Blocking inside async pipeline defeats the purpose
CompletableFuture.supplyAsync(() -> {
    return userRepository.findById(id).get();  // Blocking DB call in async thread
});

// ✅ Keep blocking calls on dedicated thread pool
@Async("dbThreadPool")
public CompletableFuture<User> findUserAsync(Long id) {
    return CompletableFuture.completedFuture(
        userRepository.findById(id).orElseThrow()
    );
}
```

```java
// ❌ Unchecked exception handling
CompletableFuture.supplyAsync(this::fetchData)
    .thenApply(this::process);  // If fetchData fails, process is skipped silently

// ✅ Handle exceptions explicitly
CompletableFuture.supplyAsync(this::fetchData)
    .thenApply(this::process)
    .exceptionally(ex -> {
        log.error("Pipeline failed", ex);
        return fallbackResult();
    });
```

---

## Virtual Threads (Java 21 / Spring Boot 3.2+)

```yaml
# Enable virtual threads globally (Spring Boot 3.2+)
spring:
  threads:
    virtual:
      enabled: true
```

```java
// ❌ Synchronized blocks pin virtual threads to carrier thread
public synchronized void process() {  // Bad with virtual threads
    heavyWork();
}

// ✅ Use ReentrantLock instead
private final ReentrantLock lock = new ReentrantLock();

public void process() {
    lock.lock();
    try { heavyWork(); }
    finally { lock.unlock(); }
}
```

**Virtual Thread Checklist:**
- Replace `synchronized` blocks with `ReentrantLock`
- Avoid `ThreadLocal` for request context — use structured concurrency scope values
- Don't pool virtual threads — create them freely
- Watch for thread-pinning in Hibernate/JDBC drivers (use drivers with virtual thread support)

---

## Review Flags

| Pattern | Risk |
|---------|------|
| Mutable instance field in `@Service`/`@Component` | Race condition |
| `if (x == null) { x = ... }` | Lazy init race condition |
| `@Async` void method | Silent exception loss |
| `@Async` self-invocation | Annotation bypassed |
| `synchronized` with virtual threads enabled | Thread pinning |
| `ThreadLocal` with virtual threads | Unexpected sharing |
| `CompletableFuture` without `exceptionally` | Silent failure |
