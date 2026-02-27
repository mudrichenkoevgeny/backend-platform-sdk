plugins {
    `java-platform`
}

dependencies {
    constraints {
        api(project(":core:common"))
        api(project(":core:observability"))
        api(project(":core:database"))
        api(project(":core:settings"))
        api(project(":core:security"))
        api(project(":core:audit"))
        api(project(":core:storage"))
        api(project(":core:events"))
        api(project(":core:crosscutting"))
        api(project(":feature:user"))
    }
}