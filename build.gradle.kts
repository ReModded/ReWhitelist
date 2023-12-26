plugins {
    kotlin("jvm") version("1.9.20")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "dev.remodded.whitelist"
version = "1.0.0"

repositories {
    maven("https://repo.remodded.dev/repository/paper/")
    maven("https://repo.remodded.dev/repository/maven-central/")
}

dependencies {
    // Velocity
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Kyorinet
    implementation("net.kyori:adventure-text-feature-pagination:4.0.0-SNAPSHOT")
}

tasks {
    val javaVersion = JavaVersion.VERSION_11

    shadowJar {
        archiveBaseName.set("ReWhitelist")
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    compileKotlin {
        kotlinOptions { jvmTarget = javaVersion.toString() }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
}
