plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.breach"
version = "2.2.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/central/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
    maven("https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.15")

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    compileOnly("net.dv8tion:JDA:5.0.0-beta.24") {
        exclude(module = "opus-java")
    }

    compileOnly("com.github.pengrad:java-telegram-bot-api:7.9.1")
    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("at.favre.lib:bcrypt:0.10.2")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("DistrictRP.jar")

    relocate("com.zaxxer.hikari", "dev.breach.DistrictRP.libs.hikari")
    relocate("org.mariadb.jdbc", "dev.breach.DistrictRP.libs.mariadb")
    relocate("com.google.gson", "dev.breach.DistrictRP.libs.gson")
    relocate("at.favre.lib.bytes", "dev.breach.DistrictRP.libs.bytes")
    relocate("at.favre.lib.crypto", "dev.breach.DistrictRP.libs.crypto")

    mergeServiceFiles()
}

tasks.build { dependsOn(tasks.shadowJar) }