# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Repository Is

This is a **Claude Code plugin template** (`spring-boot-plugin`) for enterprise Spring Boot development. It contains no application source code — only Claude Code configuration: specialized agents and reusable skills. Source: https://github.com/sumitgupta28/claude-ai-spring-boot

## Repository Structure

```
.claude-plugin/plugin.json    # Plugin metadata
.claude/agents/               # 6 specialized sub-agents
.claude/skills/               # Reusable skill prompts for Java patterns
```

### Agents (`.claude/agents/`)

| Agent | Purpose |
|-------|---------|
| `spring-boot-engineer` | Primary agent — Spring Boot 3+, microservices, reactive patterns |
| `code-reviewer` | Code quality, clean code, API contracts |
| `security-engineer` | Spring Security 6, OAuth2/JWT, OWASP |
| `devops-engineer` | CI/CD, deployment pipelines |
| `docker-expert` | Container image optimization |
| `kubernetes-specialist` | K8s workload design and troubleshooting |

### Skills (`.claude/skills/`)

Skills are invoked via slash commands or auto-loaded by context:

| Skill | Command | When used |
|-------|---------|-----------|
| `spring-boot` | `/spring-boot` | REST APIs, JPA, Security, Testing templates |
| `code-quality` | `/code-quality` | Clean code review checklist |
| `jpa-patterns` | `/jpa-patterns` | N+1, lazy loading, transaction patterns |
| `design-patterns` | `/design-patterns` | Factory, Builder, Strategy, Observer |
| `logging-patterns` | `/logging-patterns` | SLF4J, structured JSON logging, MDC |
| `gradle-build-helper` | `/gradle-build-helper` | Gradle build issues |

## Spring Boot Architecture Conventions

When generating or reviewing Spring Boot applications using this plugin, follow these conventions:

**Package structure** (base: `com.sg.services`):
```
controller/    REST endpoints — HTTP handling and validation only
service/       Business logic — owns transactions
repository/    Spring Data JPA interfaces
model/         JPA entities
dto/           Java records for request/response
config/        @Configuration classes
exception/     Custom exceptions + @RestControllerAdvice handler
```

**Mandatory patterns:**
- Constructor injection only — never `@Autowired` on fields
- `@Transactional(readOnly = true)` on service class, `@Transactional` on write methods
- `@Valid` on every `@RequestBody`
- DTOs as Java records; entities without Lombok
- Global exception handler via `@RestControllerAdvice`
- Secrets via env vars — never in `application.properties`

**Build commands for generated projects:**
```bash
./gradlew build          # Build and run all checks
./gradlew test           # Run all tests
./gradlew bootRun        # Start application
./gradlew test --tests "com.example.FooTest"  # Single test class
```

## Adding to This Plugin

To add a new skill:
1. Create `.claude/skills/<skill-name>/SKILL.md`
2. Add entry to `.claude/skills/README.md` table
