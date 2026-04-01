---
name: api-contract-review
description: REST API audit covering HTTP semantics, versioning, response codes, backward compatibility, and OpenAPI documentation. Use when reviewing REST controllers, API design, or before releasing API changes.
---

# API Contract Review Skill

Audit REST APIs for correctness, consistency, and backward compatibility.

## When to Use
- "review this REST API" / "check my endpoints"
- Before releasing a new API version
- Checking backward compatibility of changes
- API design questions

---

## HTTP Semantics Checklist

### Verb Correctness

| Verb | Rule |
|------|------|
| `GET` | Read-only, safe, idempotent. No request body. No side effects. |
| `POST` | Create resource or trigger action. Not idempotent. Returns 201 + Location header. |
| `PUT` | Full replacement. Idempotent. Client provides complete resource. |
| `PATCH` | Partial update. Body contains only changed fields. |
| `DELETE` | Remove resource. Idempotent. Returns 204 No Content. |

```java
// ❌ Verb misuse
@GetMapping("/orders/{id}/cancel")   // GET causing side effect
@PostMapping("/orders/search")        // POST for read-only query

// ✅
@PostMapping("/orders/{id}/cancel")  // POST for state-changing action
@GetMapping("/orders")               // GET with @RequestParam for filtering
```

### URL Design

```java
// ❌ Verbs in URLs
@GetMapping("/getUser/{id}")
@PostMapping("/createOrder")
@DeleteMapping("/deleteProduct/{id}")

// ✅ Nouns + HTTP verbs
@GetMapping("/users/{id}")
@PostMapping("/orders")
@DeleteMapping("/products/{id}")

// ❌ Inconsistent pluralization
@GetMapping("/user/{id}")    // singular
@GetMapping("/orders")       // plural

// ✅ Always plural for collections
@GetMapping("/users/{id}")
@GetMapping("/users")
```

### Response Codes

| Scenario | Code |
|----------|------|
| GET success | 200 OK |
| POST creates resource | 201 Created |
| PUT/PATCH success | 200 OK |
| DELETE success | 204 No Content |
| Request body validation fails | 400 Bad Request |
| Missing/invalid auth token | 401 Unauthorized |
| Valid token, insufficient role | 403 Forbidden |
| Resource not found | 404 Not Found |
| Business rule conflict (duplicate) | 409 Conflict |
| Unexpected server error | 500 Internal Server Error |

```java
// ❌ Returning 200 for everything
@PostMapping("/users")
public ResponseEntity<User> create(...) {
    return ResponseEntity.ok(userService.create(request));  // Should be 201
}

// ✅
@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)
public User create(@Valid @RequestBody UserRequest request) { }
```

---

## Versioning

```java
// ✅ URL path versioning (recommended for Spring Boot)
@RequestMapping("/api/v1/products")

// ❌ No versioning — breaking changes break all clients
@RequestMapping("/products")
```

Breaking changes that require a new version:
- Removing or renaming a field in the response
- Changing a field type
- Removing an endpoint
- Changing required/optional status of request fields

Non-breaking changes (no version bump needed):
- Adding optional fields to response
- Adding optional request parameters
- Adding new endpoints

---

## Request Validation

```java
// ❌ No validation
@PostMapping("/users")
public User create(@RequestBody UserRequest request) { }

// ✅ Always validate
@PostMapping("/users")
public User create(@Valid @RequestBody UserRequest request) { }

public record UserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 2, max = 50) String name,
    @Min(0) @Max(150) Integer age
) {}
```

---

## Response Body Design

```java
// ❌ Exposing JPA entity (leaks internals, serialization issues)
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}

// ✅ Return DTO
@GetMapping("/{id}")
public UserResponse getUser(@PathVariable Long id) {
    return userService.findById(id);
}

public record UserResponse(Long id, String name, String email) {}
```

**Flags:**
- Jackson-annotated entities returned directly from controllers
- `@JsonIgnore` on entity fields (signals leaking internal model)
- `password`, `secret`, `token` fields in response
- Non-deterministic field ordering

---

## Backward Compatibility Audit

When reviewing a change to an existing API:

```
1. Were any response fields removed or renamed?     → Breaking
2. Did any field type change (String → Integer)?    → Breaking
3. Did any previously optional field become required? → Breaking
4. Was an endpoint removed?                         → Breaking
5. Were new optional fields added to response?      → Safe
6. Were new optional query params added?            → Safe
7. Was a new endpoint added?                        → Safe
```

---

## Documentation Check

```java
// ✅ OpenAPI annotations for important APIs
@Operation(summary = "Create a new user",
           description = "Creates a user and returns 201 with Location header")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "User created"),
    @ApiResponse(responseCode = "400", description = "Validation failure"),
    @ApiResponse(responseCode = "409", description = "Email already in use")
})
@PostMapping("/users")
public ResponseEntity<UserResponse> create(...) { }
```

---

## Review Output Format

```markdown
### Verb / URL Issues
- `POST /orders/search` — Use `GET /orders` with query params for searches.

### Status Code Issues
- `POST /users` returns 200 — should return 201 Created.

### Validation Missing
- `PUT /products/{id}` — `@RequestBody` not validated with `@Valid`.

### Breaking Changes Detected
- Response field `user_name` renamed to `username` — breaking for existing clients.
```
