---
name: maven-dependency-audit
description: Audit Maven/Gradle dependencies for outdated versions, vulnerabilities, and unused/duplicate dependencies. Use when reviewing pom.xml or build.gradle, or before a release.
---

# Maven Dependency Audit Skill

Audit project dependencies for version staleness, security vulnerabilities, and bloat.

## When to Use
- "check for outdated dependencies"
- "audit our dependencies" / "find CVEs in dependencies"
- Before a major release or security review
- Reviewing pom.xml or build.gradle changes

---

## Maven Dependency Commands

```bash
# Check for outdated versions
./mvnw versions:display-dependency-updates

# Check for outdated plugin versions
./mvnw versions:display-plugin-updates

# Find dependency vulnerabilities (OWASP)
./mvnw dependency:check -Dowasp.check=true
# Or via OWASP plugin
./mvnw org.owasp:dependency-check-maven:check

# Show dependency tree (find duplicates/conflicts)
./mvnw dependency:tree

# Find unused declared / used undeclared dependencies
./mvnw dependency:analyze

# Show effective POM (resolved versions)
./mvnw help:effective-pom
```

## Gradle Dependency Commands

```bash
# Check for outdated versions (requires ben-manes plugin)
./gradlew dependencyUpdates

# Dependency tree
./gradlew dependencies
./gradlew dependencies --configuration compileClasspath

# Check for vulnerabilities (requires OWASP plugin)
./gradlew dependencyCheckAnalyze

# Find unused dependencies
./gradlew projectReport  # or use 'com.autonomousapps.dependency-analysis' plugin
```

---

## Vulnerability Check Setup

### Maven (pom.xml)
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- Fail on HIGH/CRITICAL -->
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Gradle (build.gradle)
```groovy
plugins {
    id 'org.owasp.dependencycheck' version '9.0.9'
}

dependencyCheck {
    failBuildOnCVSS = 7  // Fail on CVSS >= 7 (HIGH)
    formats = ['HTML', 'JSON']
}
```

---

## Version Management Best Practices

### Maven: Use BOM for version alignment

```xml
<!-- ✅ Import Spring Boot BOM — manages all Spring versions -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Then omit versions for managed deps -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <!-- No version needed -->
</dependency>
```

### Gradle: Platform dependencies

```groovy
// ✅ Import Spring Boot BOM
dependencies {
    implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.0')
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'  // No version
}
```

---

## Audit Checklist

### Staleness
- [ ] Spring Boot parent at latest patch for current minor
- [ ] No dependency more than 2 major versions behind
- [ ] Jackson, Hibernate, Tomcat at versions shipped by Spring Boot BOM
- [ ] No overridden BOM versions unless justified

### Duplication & Conflict
- [ ] No two versions of the same artifact in dependency tree
- [ ] No `javax.*` and `jakarta.*` mixed (Spring Boot 3 requires `jakarta.*` only)
- [ ] No `spring-boot-starter-*` + direct Spring Framework dependency (double-managing versions)

### Unused / Unnecessary
- [ ] No test-scoped deps in compile scope
- [ ] No `provided` deps that are already transitive
- [ ] Remove Lombok if project uses Java records exclusively

### Scope
```xml
<!-- ❌ Test dependency in compile scope -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <!-- Missing: <scope>test</scope> -->
</dependency>

<!-- ✅ -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Severity Triage

| Finding | Action |
|---------|--------|
| CVE CVSS ≥ 9.0 (Critical) | Upgrade immediately — block release |
| CVE CVSS 7.0–8.9 (High) | Upgrade this sprint |
| CVE CVSS 4.0–6.9 (Medium) | Schedule upgrade within 30 days |
| CVE CVSS < 4.0 (Low) | Log and monitor |
| 2+ major versions behind | Plan upgrade |
| 1 major version behind | Document and schedule |
| Unused declared dependency | Remove |
