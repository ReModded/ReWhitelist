import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    id("org.jetbrains.kotlin.plugin.serialization") version (libs.versions.kotlin)
    alias(libs.plugins.runTask)
}

group = "dev.remodded"
version = "1.0.3"

repositories {
    maven("https://repo.remodded.dev/repository/PaperMC/")
    maven("https://repo.remodded.dev/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots"
    }
    maven("https://repo.opencollab.dev/main/") {
        name = "GeyserMC"
    }
}

dependencies {
    compileOnly(libs.velocity.api)

    implementation(libs.kyori.pagginate)

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
}

tasks {

    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }

    shadowJar {
        archiveBaseName.set("ReWhitelist")
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filter<ReplaceTokens>(
            "tokens" to mapOf("version" to project.version)
        )
    }
}
