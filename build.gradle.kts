plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.breach"
version = "1.0.0"
description = "Core ufficiale della DistrictRP"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.15")

    implementation("net.dv8tion:JDA:5.0.0-beta.24") {
        exclude(module = "opus-java")
    }
    implementation("com.github.pengrad:java-telegram-bot-api:7.9.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("DistrictRP")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    relocate("net.dv8tion.jda", "dev.breach.DistrictRP.lib.jda")
    relocate("com.pengrad.telegrambot", "dev.breach.DistrictRP.lib.telegram")
    relocate("okhttp3", "dev.breach.DistrictRP.lib.okhttp3")
    relocate("okio", "dev.breach.DistrictRP.lib.okio")
    relocate("com.google.gson", "dev.breach.DistrictRP.lib.gson")

    minimize {
        exclude(dependency("net.dv8tion:.*:.*"))
        exclude(dependency("com.github.pengrad:.*:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}