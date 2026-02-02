# backend-platform-sdk
A modular foundational SDK for building scalable Kotlin/Ktor microservices. Provides pre-configured core infrastructure for observability, database management, security, and shared business features.

# Integration Steps
To initialize the SDK, you must provide the application metadata (name and version). This is done by implementing the AppInfo interface and binding it in your dependency injection graph.

### 1. Implement AppInfo: Create a class that implements AppInfo.

```kotlin
@Singleton
class BuildConfigAppInfo @Inject constructor() : AppInfo {
    override val version: String = BuildConfig.VERSION
    override val appName: String = BuildConfig.APP_NAME
}
```

### 2. Provide the Binding. Register the implementation in your Dagger module

```kotlin
@Module
interface AppModule {
    @Binds
    @Singleton
    fun bindAppInfo(impl: BuildConfigAppInfo): AppInfo
}
```