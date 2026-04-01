---
name: gradle-build-helper
description: Gradle build troubleshooting and configuration for Spring Boot Java projects. Use when diagnosing build failures, configuring dependencies, or optimizing Gradle builds.
---

# Gradle Build Helper Skill

Diagnose and fix Gradle build issues for Spring Boot Java projects.

## When to Use
- Build failures or dependency resolution errors
- "configure Gradle for Spring Boot"
- Optimizing build speed
- Setting up multi-module projects

---

## Essential Commands

```bash
# Run all tests
./gradlew test

# Build without tests
./gradlew build -x test

# Clean and rebuild
./gradlew clean build

# Run a single test class
./gradlew test --tests "com.example.UserServiceTest"

# Run a single test method
./gradlew test --tests "com.example.UserServiceTest.createUser_validRequest_returnsUser"

# Show dependency tree
./gradlew dependencies --configuration compileClasspath

# Show why a dependency is included
./gradlew dependencyInsight --dependency spring-security-core --configuration compileClasspath

# Check for outdated dependencies (requires ben-manes plugin)
./gradlew dependencyUpdates

# Show all available tasks
./gradlew tasks

# Debug build issues with full stack trace
./gradlew build --stacktrace --info
```

---

## Standard Spring Boot build.gradle

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    runtimeOnly 'org.postgresql:postgresql'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## Common Dependency Resolution Errors

### Version Conflict

```
Could not resolve com.fasterxml.jackson.core:jackson-databind:2.14.0
  > Could not resolve 2.14.0
```

```groovy
// ✅ Force a version via BOM or resolution strategy
configurations.all {
    resolutionStrategy.eachDependency { DependencyResolveDetails details ->
        if (details.requested.group == 'com.fasterxml.jackson.core') {
            details.useVersion '2.16.1'
        }
    }
}
```

### Duplicate Classes

```
Execution failed for task ':compileJava'.
> error: duplicate class javax.annotation.Nullable
```

```groovy
// ✅ Exclude the conflicting transitive dependency
dependencies {
    implementation('some.library:artifact:1.0') {
        exclude group: 'com.google.code.findbugs', module: 'jsr305'
    }
}
```

### javax vs jakarta Conflict (Spring Boot 3.x)

```
ClassNotFoundException: javax.persistence.Entity
```

```groovy
// ✅ Ensure no Spring Boot 2.x / Hibernate 5.x artifacts mixed in
// Check with:
// ./gradlew dependencyInsight --dependency hibernate-core
// Look for 5.x versions being pulled in — exclude or force to 6.x
dependencies {
    implementation('some.lib:artifact:1.0') {
        exclude group: 'org.hibernate', module: 'hibernate-core'
    }
    implementation 'org.hibernate.orm:hibernate-core:6.4.0.Final'
}
```

---

## Build Performance

```groovy
// gradle.properties — enable parallel execution and build cache
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError

// ✅ Test JVM reuse across tests
tasks.named('test') {
    useJUnitPlatform()
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
    forkEvery = 100  // Restart JVM every 100 tests to prevent memory leaks
}
```

---

## Multi-Module Project Structure

```groovy
// settings.gradle
rootProject.name = 'my-app'
include 'api', 'service', 'data'

// build.gradle (root)
subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    dependencyManagement {
        imports {
            mavenBom "org.springframework.boot:spring-boot-dependencies:3.3.0"
        }
    }

    repositories { mavenCentral() }

    tasks.named('test') { useJUnitPlatform() }
}

// api/build.gradle
dependencies {
    implementation project(':service')
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

---

## Dependency Scope Quick Reference

| Configuration | Purpose |
|--------------|---------|
| `implementation` | Compile + runtime; not exposed to consumers |
| `api` | Compile + runtime; exposed to consumers (library projects) |
| `compileOnly` | Compile only (e.g., annotation processors, Lombok) |
| `runtimeOnly` | Runtime only (e.g., JDBC drivers, DB connectors) |
| `testImplementation` | Test compile + runtime |
| `testRuntimeOnly` | Test runtime only (e.g., JUnit platform launcher) |
| `annotationProcessor` | Compile-time annotation processing |

```groovy
// Typical test dependencies
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
testImplementation 'org.testcontainers:postgresql:1.19.3'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```
