plugins {
    kotlin("jvm") version "2.2.10"
    id("com.gradleup.shadow") version "9.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "com.ratger.acreative"
version = "1.2.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.evokegames.gg/snapshots")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    implementation("com.github.Tofaa2.EntityLib:spigot:2.4.11")
    compileOnly("com.github.retrooper:packetevents-spigot:2.9.4")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val plainJarName = "${project.name}-${project.version}_1.21.1.jar"
val shadowJarName = "${project.name}-${project.version}_1.21.1-shadow.jar"

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    compileJava {
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.name,
            "version" to project.version
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveFileName.set(shadowJarName)
        minimize()
        relocate("kotlin", "${project.group}.lib.kotlin")
        relocate("me.tofaa.entitylib", "${project.group}.lib.entitylib")
    }

    jar {
        archiveFileName.set(plainJarName)
    }

    build {
        dependsOn(shadowJar)
    }
}