---
name: issue-triage
description: GitHub issue triage and categorization for Java/Spring Boot projects. Use when classifying issues, assigning labels, estimating effort, or prioritizing a backlog.
---

# Issue Triage Skill

Structured approach to classifying, labeling, and prioritizing GitHub issues.

## When to Use
- "triage this issue" / "categorize issues"
- "what label should this get" / "is this a bug or feature"
- Reviewing a backlog and assigning priorities

---

## Triage Checklist

For each issue, determine:

1. **Type** — bug, feature, performance, documentation, question
2. **Severity** (bugs) — critical, high, medium, low
3. **Priority** — P0, P1, P2, P3
4. **Effort** — S (hours), M (1-2 days), L (3-5 days), XL (week+)
5. **Affected layer** — API, service, repository, security, config, infra

---

## Label Taxonomy

### Type Labels
| Label | Description |
|-------|-------------|
| `bug` | Something is broken or behaving incorrectly |
| `feature` | New capability requested |
| `enhancement` | Improvement to existing functionality |
| `performance` | Slowness, memory, or throughput issue |
| `security` | Security vulnerability or hardening request |
| `documentation` | Docs missing or incorrect |
| `question` | Needs clarification, not a code change |
| `chore` | Dependency update, refactor, tech debt |

### Severity Labels (bugs only)
| Label | Criteria |
|-------|----------|
| `severity:critical` | Data loss, security breach, service down |
| `severity:high` | Feature broken for all users, no workaround |
| `severity:medium` | Feature broken for some users, workaround exists |
| `severity:low` | Minor inconvenience, cosmetic |

### Priority Labels
| Label | Meaning |
|-------|---------|
| `P0` | Fix immediately — production incident |
| `P1` | Fix this sprint |
| `P2` | Fix next sprint |
| `P3` | Backlog — nice to have |

### Layer Labels
`layer:api` `layer:service` `layer:data` `layer:security` `layer:config` `layer:infra`

---

## Triage Decision Tree

```
Is it broken?
├── Yes → bug
│   ├── Data loss / security breach → severity:critical, P0
│   ├── Core feature unusable → severity:high, P1
│   ├── Workaround available → severity:medium, P2
│   └── Minor / cosmetic → severity:low, P3
└── No → feature or enhancement
    ├── Aligns with roadmap → P1 or P2
    └── Nice-to-have → P3
```

---

## Triage Response Template

```markdown
Thanks for the report!

**Type:** Bug / Feature / Enhancement
**Severity:** Critical / High / Medium / Low
**Priority:** P0 / P1 / P2 / P3
**Effort estimate:** S / M / L / XL
**Affected layer:** api / service / data / security

**Notes:**
[Reproduction confirmed? Missing information? Related issues?]

**Next steps:**
- [ ] Reproduce in local environment
- [ ] Identify root cause
- [ ] Implement fix
```

---

## Common Spring Boot Issue Patterns

| Symptom | Likely Cause | Label |
|---------|-------------|-------|
| `LazyInitializationException` | Missing `@Transactional` or EAGER fetch | `bug`, `layer:data` |
| `401 Unauthorized` on valid token | Security config misconfiguration | `bug`, `layer:security` |
| Slow endpoint (>1s) | N+1 query or missing index | `performance`, `layer:data` |
| `NullPointerException` in service | Missing null check / Optional misuse | `bug`, `layer:service` |
| `HttpMessageNotReadableException` | Request body validation mismatch | `bug`, `layer:api` |
| Context startup failure | Bean definition conflict | `bug`, `layer:config` |
