plugins {
    alias(libs.plugins.kotlin.jvm)
}

/*
 * Domain module: pure Kotlin, no Android dependencies.
 * The accounting engine, models and use cases live here so the same rules can be
 * compiled for iOS later (Kotlin Multiplatform target) without rewriting business logic.
 */
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnit()
}
