plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "it.districtrp"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly(files("libs/DistrictRP.jar"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("at.favre.lib:bcrypt:0.10.2")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.gson", "it.districtrp.staffpanel.libs.gson")
    relocate("at.favre.lib.bytes", "it.districtrp.staffpanel.libs.bytes")
    relocate("at.favre.lib.crypto", "it.districtrp.staffpanel.libs.crypto")
}

tasks.build { dependsOn(tasks.shadowJar) }