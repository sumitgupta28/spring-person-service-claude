---
name: changelog-generator
description: Generate changelogs from git commit history for Java/Spring Boot projects. Use when creating release notes, CHANGELOG.md entries, or summarizing changes between versions.
---

# Changelog Generator Skill

Generate structured changelogs from conventional commits.

## When to Use
- "generate changelog" / "write release notes"
- "what changed between v1.0 and v2.0"
- Preparing a release and documenting changes

---

## Workflow

1. Run `git log <from>..<to> --oneline --no-merges` to get commits
2. Group by type: `feat`, `fix`, `perf`, `refactor`, `chore`
3. Format into Keep-a-Changelog sections
4. Highlight breaking changes prominently

---

## Git Commands

```bash
# Commits since last tag
git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-merges

# Commits between two tags
git log v1.0.0..v2.0.0 --oneline --no-merges

# Full commit messages (for body/footer context)
git log v1.0.0..v2.0.0 --no-merges --format="%H %s%n%b"
```

---

## CHANGELOG.md Format (Keep-a-Changelog)

```markdown
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [2.1.0] - 2026-04-01

### ⚠️ Breaking Changes
- Renamed `/api/v1/users` to `/api/v1/accounts` — update all clients

### Added
- Pagination support on `GET /orders` endpoint (#78)
- JWT refresh token endpoint `POST /auth/refresh` (#81)
- `OrderService.findByStatus()` with filtering and sorting

### Fixed
- `NullPointerException` in `UserService` when email field is null (#74)
- Incorrect HTTP 500 returned for validation errors (now returns 400)

### Changed
- `ProductRepository.findAll()` now returns `Page<Product>` instead of `List<Product>`
- Upgraded Spring Boot from 3.2.1 to 3.3.0

### Performance
- Eliminated N+1 query in `OrderController.list()` via JOIN FETCH

## [2.0.0] - 2026-01-15
...
```

---

## Section Mapping

| Commit type | Changelog section |
|-------------|------------------|
| `feat` | Added |
| `fix` | Fixed |
| `perf` | Performance |
| `refactor` | Changed (if user-visible) |
| `chore(deps)` | Changed (dependency version) |
| `docs` | (usually omit) |
| `!` suffix or `BREAKING CHANGE` footer | ⚠️ Breaking Changes |

---

## Rules

- **User-facing only:** Omit internal refactors, test changes, and CI changes unless they affect the public API
- **Plain language:** Write for users/consumers, not developers
- **Link issues:** Include `(#123)` references for traceability
- **Breaking changes first:** Always place at the top of the version section
