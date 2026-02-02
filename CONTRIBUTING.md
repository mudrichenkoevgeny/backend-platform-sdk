# Development and Publishing

## Dependency Analysis

The project uses the `com.autonomousapps.dependency-analysis` plugin to maintain a clean classpath and optimize the artifact size.

### 1. Project-Wide Health Check
To perform a comprehensive audit of unused, transitive, and misconfigured dependencies across the entire project, run:

```bash
./gradlew buildHealth
```

To run the audit for a single module only, use the projectHealth task:
```bash
./gradlew :<module-name>:projectHealth
```

Example for core:common:
```bash
./gradlew :core:common:projectHealth
```

### 2. Dependency Insight
If the health check warns that a dependency "should be declared directly", use this command for a specific module to find which top-level library brought it:
```bash
./gradlew <module-name>:dependencyInsight --dependency <library-name> --configuration compileClasspath
```

Example for core:common and org.jetbrains.kotlinx:kotlinx-serialization-json:

```bash
./gradlew core:common:dependencyInsight --dependency org.jetbrains.kotlinx:kotlinx-serialization-json --configuration compileClasspath
```

To generate a complete, searchable text report of all dependencies in a specific module:
```bash
./gradlew :<module-name>:dependencies --configuration compileClasspath > build/reports/<module-name>-dependencies-report.txt
```

Example for core:common:
```bash
./gradlew :core:common:dependencies --configuration compileClasspath > build\reports\core-common-dependencies-report.txt
```

## Artifact Deployment

The project utilizes the `com.vanniktech.maven.publish` plugin for artifact management.

### 1. Remote Staging
Uploads all publications to the remote staging repository (Maven Central) without performing a final release. Use this for manual verification in the repository manager:

```bash
./gradlew publishAllPublicationsToMavenCentralRepository
```

### 2. Full Release Cycle
Performs a complete deployment workflow, including uploading, closing the staging repository, and releasing artifacts to Maven Central:

```bash
./gradlew publishAndReleaseToMavenCentral
```