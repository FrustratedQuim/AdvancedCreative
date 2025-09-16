plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "org.acreative"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    // FancyNPC & FancyHolograms
//    maven("https://repo.fancyinnovations.com/releases")

    // PacketEvents
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }

    // EntityLib
    maven(url = "https://maven.evokegames.gg/snapshots")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("com.github.Tofaa2.EntityLib:spigot:master-6fba8ea5fdc7880d1c62c3428f562fea2745b58b") // 2.4.11

//    compileOnly("de.oliver:FancyNpcs:2.7.0")
//    compileOnly("de.oliver:FancyHolograms:2.7.0")
//    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.9.4")
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
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveFileName.set(shadowJarName)
        minimize()
        relocate("kotlin", "${project.group}.kotlin")
        relocate("io.papermc.lib", "${project.group}.paperlib")
        relocate("org.jetbrains.annotations", "${project.group}.jetbrains.annotations")
        relocate("org.intellij.lang.annotations", "${project.group}.intellij.lang.annotations")
    }

    jar {
        archiveFileName.set(plainJarName)
    }

    build {
        dependsOn(shadowJar)
    }

    val copyPluginJar by registering(Copy::class) {
        from("$buildDir/libs/$shadowJarName")
        into("C:/Users/Home/Desktop/Архив/Minecraft/Сервера/Creative_1.21.1/plugins")
        doLast {
            println("Plugin moved to server plugins.")
        }
    }

    build {
        finalizedBy(copyPluginJar)
    }
}
