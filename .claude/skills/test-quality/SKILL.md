---
name: test-quality
description: JUnit 5 + AssertJ testing patterns for Spring Boot. Use when writing or reviewing tests, improving test coverage, or fixing flaky tests.
---

# Test Quality Skill

JUnit 5 and AssertJ patterns for reliable, readable Spring Boot tests.

## When to Use
- "write tests for this" / "review my tests"
- Fixing flaky or brittle tests
- Choosing the right test type (unit vs slice vs integration)

---

## Test Type Selection

| Test Type | Annotation | Scope | Speed |
|-----------|-----------|-------|-------|
| Unit | (none / `@ExtendWith(MockitoExtension.class)`) | Single class | Fast |
| Controller slice | `@WebMvcTest` | Controller + web layer | Fast |
| Repository slice | `@DataJpaTest` | JPA + in-memory DB | Medium |
| Full integration | `@SpringBootTest` | Full context | Slow |

**Rule:** Use the narrowest test type that covers the behaviour.

---

## Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_validRequest_savesAndReturnsOrder() {
        // Arrange
        var request = new CreateOrderRequest("item-1", 2);
        var saved = new Order(1L, "item-1", 2, OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        // Act
        Order result = orderService.create(request);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }
}
```

---

## Controller Slice Tests (@WebMvcTest)

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_validRequest_returns201() throws Exception {
        var request = new CreateOrderRequest("item-1", 2);
        var response = new OrderResponse(1L, "item-1", 2, "PENDING");
        when(orderService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"quantity":2}"""))
            .andExpect(status().isBadRequest());
    }
}
```

---

## Repository Slice Tests (@DataJpaTest)

```java
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByStatus_returnsMatchingOrders() {
        orderRepository.save(new Order("item-1", 1, OrderStatus.PENDING));
        orderRepository.save(new Order("item-2", 2, OrderStatus.SHIPPED));

        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getItemId()).isEqualTo("item-1");
    }
}
```

---

## Integration Tests (Testcontainers)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndRetrieveOrder_fullFlow_succeeds() {
        var request = new CreateOrderRequest("item-1", 2);
        var created = restTemplate.postForEntity("/api/v1/orders", request, OrderResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().id();

        var retrieved = restTemplate.getForEntity("/api/v1/orders/" + id, OrderResponse.class);
        assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retrieved.getBody().itemId()).isEqualTo("item-1");
    }
}
```

---

## AssertJ Best Practices

```java
// ❌ JUnit assertions — poor error messages
assertEquals(expected, actual);
assertTrue(list.size() == 3);
assertNotNull(result);

// ✅ AssertJ — descriptive failure messages
assertThat(actual).isEqualTo(expected);
assertThat(list).hasSize(3);
assertThat(result).isNotNull();

// ✅ Chained assertions
assertThat(user)
    .isNotNull()
    .extracting(User::getName, User::getEmail)
    .containsExactly("Alice", "alice@example.com");

// ✅ Collection assertions
assertThat(orders)
    .hasSize(2)
    .extracting(Order::getStatus)
    .containsOnly(OrderStatus.PENDING);

// ✅ Exception assertions
assertThatThrownBy(() -> service.findById(-1L))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("ID must be positive");
```

---

## Test Anti-Patterns

```java
// ❌ Tests depending on each other (shared mutable state)
@TestMethodOrder(OrderAnnotation.class)
class OrderServiceTest {
    static Long createdId;

    @Test @Order(1)
    void create() { createdId = service.create(...).getId(); }

    @Test @Order(2)
    void read() { service.findById(createdId); }  // Depends on create
}

// ✅ Each test is independent
@Test
void findById_existingOrder_returnsOrder() {
    Order order = orderRepository.save(new Order(...));
    assertThat(service.findById(order.getId())).isNotNull();
}
```

```java
// ❌ Testing implementation details
verify(mockRepo, times(1)).save(any());  // Brittle — tied to internal calls

// ✅ Test observable behaviour
assertThat(service.create(request).getId()).isNotNull();
```

```java
// ❌ Overly broad mock
when(service.create(any())).thenReturn(any());

// ✅ Specific inputs and outputs
when(service.create(eq(request))).thenReturn(expectedOrder);
```

---

## Naming Convention

```
methodName_scenario_expectedOutcome

createOrder_validRequest_returns201
findById_nonExistentId_throwsEntityNotFoundException
calculateDiscount_goldMember_appliesTwentyPercent
```
