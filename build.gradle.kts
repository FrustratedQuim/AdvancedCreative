plugins {
    kotlin("jvm") version "1.9.23"
    id("com.gradleup.shadow") version "8.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "org.acreative"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.fancyinnovations.com/releases")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    implementation("net.kyori:adventure-text-minimessage:4.23.0")
    compileOnly("de.oliver:FancyNpcs:2.7.0")
    compileOnly("de.oliver:FancyHolograms:2.7.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.23")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val plainJarName = "${project.name}-${project.version}_1.21.1.jar"
val shadowJarName = "${project.name}-${project.version}_1.21.1-shadow.jar"

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
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
        from("$buildDir/libs/$plainJarName")
        into("C:/Users/Home/Desktop/Архив/Minecraft/Сервера/Creative_1.21.1/plugins")
        doLast {
            println("Plugin moved to server plugins.")
        }
    }

    build {
        finalizedBy(copyPluginJar)
    }
}
