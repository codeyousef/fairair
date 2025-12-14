# Project Guidelines

## 1. Build/Configuration Instructions

### Prerequisites
- **JDK 17 or higher** is required. Ensure `JAVA_HOME` is set correctly.
- Android SDK (for `apps-kmp` Android target).

### Setup
1. Clone the repository.
2. Ensure you have a valid `local.properties` file in the root directory with the path to your Android SDK if you are working on the Android app:
   ```properties
   sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
   ```
   (Note: Windows path style)

### Building the Project
To build the entire project, run:
```bash
./gradlew build
```
*(On Windows, if `gradlew.bat` is missing, use Git Bash or similar to run `./gradlew`, or execute via `java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build`)*

## 2. Testing Information

### Configuring and Running Tests

**Backend (Spring Boot):**
Run all tests for the backend:
```bash
./gradlew :backend-spring:test
```

**Mobile Apps (KMP):**
Run checks for the Multiplatform module:
```bash
./gradlew :apps-kmp:check
```
Or specific targets:
- Android: `./gradlew :apps-kmp:testDebugUnitTest`
- iOS: `./gradlew :apps-kmp:iosX64Test` (requires macOS)

### Adding New Tests

**Backend Tests:**
- Location: `backend-spring/src/test/kotlin/`
- Framework: JUnit 5
- Naming convention: Ends with `Test.kt`
- Inherit from `E2ETestBase` for integration/E2E tests requiring full context.

**Example: Creating a Simple Unit Test**

1. Create a file `backend-spring/src/test/kotlin/com/fairair/SimpleTest.kt`:

```kotlin
package com.fairair

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class SimpleTest {
    @Test
    fun `simple test`() {
        assertTrue(true)
    }
}
```

2. Run the specific test:
```bash
./gradlew :backend-spring:test --tests "com.fairair.SimpleTest"
```

## 3. Additional Development Information

### Code Style
- Follow standard Kotlin coding conventions.
- Indentation: 4 spaces.
- No explicit linter configuration (e.g. `.editorconfig`) was found, so please mirror existing patterns.
- Existing tests use descriptive function names with backticks (e.g., \`simple test\`).

### Troubleshooting
- **Gradle Wrapper on Windows**: If `gradlew.bat` is missing, you can invoke the wrapper jar directly:
  ```powershell
  java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain tasks
  ```
  Ensure you are using Java 17+.
