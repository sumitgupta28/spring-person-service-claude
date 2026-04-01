---
name: security-audit
description: OWASP Top 10 security audit for Spring Boot — injection, authentication, authorization, secrets, and misconfiguration. Use when reviewing security-sensitive code or hardening an application.
---

# Security Audit Skill

OWASP Top 10 review for Spring Boot applications.

## When to Use
- "audit for security" / "check for vulnerabilities"
- Reviewing authentication/authorization code
- Before a security review or penetration test
- Adding new endpoints that handle sensitive data

---

## Injection (A03)

### SQL Injection

```java
// ❌ SQL injection via string concatenation
@Query("SELECT u FROM User u WHERE u.name = '" + name + "'")

// ❌ Native query with concatenation
@Query(value = "SELECT * FROM users WHERE name = '" + name + "'", nativeQuery = true)

// ✅ Parameterized JPQL
@Query("SELECT u FROM User u WHERE u.name = :name")
List<User> findByName(@Param("name") String name);

// ✅ Spring Data method naming (auto-parameterized)
List<User> findByName(String name);
```

### Command Injection

```java
// ❌ User input in shell command
Runtime.getRuntime().exec("convert " + userInput + " output.png");

// ✅ Use ProcessBuilder with separate args (no shell interpretation)
new ProcessBuilder("convert", sanitizedInput, "output.png").start();
```

---

## Authentication & Authorization (A01, A07)

```java
// ❌ No method-level security
@GetMapping("/admin/users")
public List<User> getAllUsers() { }  // Anyone authenticated can call this

// ✅ Method security
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public List<User> getAllUsers() { }
```

```java
// ❌ Hardcoded credentials
private static final String ADMIN_PASSWORD = "admin123";

// ✅ Environment variable
@Value("${admin.password}")
private String adminPassword;
```

```java
// ❌ JWT secret too short / hardcoded
private static final String SECRET = "secret";

// ✅ Externalized, high-entropy secret
@Value("${jwt.secret}")  // Min 256-bit random value
private String jwtSecret;
```

---

## Sensitive Data Exposure (A02)

```java
// ❌ Password in response DTO
public record UserResponse(Long id, String name, String password) {}

// ✅ Never include sensitive fields in responses
public record UserResponse(Long id, String name) {}
```

```java
// ❌ Logging sensitive data
log.info("Login attempt for user {} with password {}", email, password);

// ✅
log.info("Login attempt for user {}", email);
```

```java
// ❌ Sensitive data in URL (visible in logs, browser history)
@GetMapping("/reset?token={token}")  // Token in query string

// ✅ Token in path or request body (POST)
@PostMapping("/password-reset")
public void resetPassword(@RequestBody PasswordResetRequest request) { }
```

---

## Security Misconfiguration (A05)

```java
// ❌ CSRF disabled without justification
http.csrf(AbstractHttpConfigurer::disable);

// ✅ Disable only for stateless REST APIs (with JWT)
http.csrf(AbstractHttpConfigurer::disable)  // OK for stateless JWT API
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
```

```java
// ❌ All endpoints permitted
http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

// ✅ Explicit allowlist
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/api/v1/auth/**").permitAll()
    .anyRequest().authenticated());
```

```java
// ❌ Actuator fully exposed
management.endpoints.web.exposure.include=*  // Exposes /actuator/env, /actuator/heapdump

// ✅ Expose only health and info
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

---

## Input Validation (A03)

```java
// ❌ No validation on incoming data
@PostMapping("/users")
public User create(@RequestBody UserRequest request) { }

// ✅ Validate at boundary
public record UserRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String username
) {}

@PostMapping("/users")
public User create(@Valid @RequestBody UserRequest request) { }
```

---

## Path Traversal (A01)

```java
// ❌ User-controlled file path
String filename = request.getParameter("file");
new FileInputStream("/uploads/" + filename);  // ../../etc/passwd

// ✅ Sanitize and validate
String filename = Paths.get(request.getParameter("file"))
    .getFileName()  // Strips directory components
    .toString();
Path resolved = uploadDir.resolve(filename).normalize();
if (!resolved.startsWith(uploadDir)) {
    throw new SecurityException("Path traversal attempt");
}
```

---

## CORS Misconfiguration

```java
// ❌ Wildcard origin in production
@CrossOrigin(origins = "*")

// ✅ Explicit origins
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

---

## Audit Checklist

| Category | Check |
|----------|-------|
| Injection | No string concat in queries; no shell exec with user input |
| Auth | All non-public endpoints require authentication |
| Authz | Sensitive endpoints have `@PreAuthorize` |
| Secrets | No hardcoded passwords, tokens, keys in source |
| Logging | No passwords/tokens/PII in log statements |
| Response | No sensitive fields (password, secret) in DTOs |
| Actuator | Only health/info endpoints exposed publicly |
| CORS | No wildcard origins in production config |
| Validation | `@Valid` on all `@RequestBody` parameters |
| HTTPS | `server.ssl.*` configured or handled by reverse proxy |
