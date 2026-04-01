---
name: git-commit
description: Conventional commit messages for Java/Spring Boot projects. Use when committing changes, writing commit messages, or setting up commit conventions.
---

# Git Commit Skill

Conventional Commits format for Java projects.

## When to Use
- "commit these changes" / "write a commit message"
- "what should my commit say"
- Setting up commit conventions for a project

---

## Conventional Commits Format

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

**Types:**

| Type | Use For |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change that is neither fix nor feature |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `chore` | Build process, dependency updates |
| `perf` | Performance improvement |
| `ci` | CI/CD pipeline changes |

**Scopes (Spring Boot context):**

| Scope | Example |
|-------|---------|
| `api` | REST controller changes |
| `service` | Business logic changes |
| `repo` | Repository/data access changes |
| `security` | Authentication/authorization |
| `config` | Configuration changes |
| `db` | Database migrations |

---

## Examples

```
# Feature
feat(api): add pagination to GET /users endpoint

# Bug fix with body
fix(service): prevent NPE when user email is null

UserService.findByEmail() returned null when email was not set,
causing NullPointerException in downstream processing.

Fixes #42

# Refactor
refactor(repo): replace manual SQL with Spring Data query methods

# Test
test(service): add unit tests for OrderService.create()

# Dependency update
chore(deps): upgrade Spring Boot to 3.3.0

# Breaking change
feat(api)!: rename /users to /accounts

BREAKING CHANGE: All /users endpoints have moved to /accounts.
Update clients accordingly.
```

---

## Rules

- **Summary line:** 72 characters max, imperative mood ("add" not "added")
- **Body:** Explain *what* and *why*, not *how*; wrap at 72 chars
- **Footer:** Reference issues (`Fixes #123`, `Closes #456`), note breaking changes
- **Scope:** Use the layer or module name, lowercase, singular

## Anti-patterns

```
# ❌ Too vague
fix: fix bug
chore: update stuff
feat: changes

# ❌ Past tense
feat: added new endpoint
fix: fixed the thing

# ✅ Clear, imperative
fix(service): handle null email in UserService.findByEmail
feat(api): add POST /orders endpoint with validation
refactor(repo): extract custom queries to named constants
```
