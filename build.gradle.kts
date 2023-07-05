plugins {
    kotlin("jvm") version "1.8.21"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "net.fish"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.4")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.slf4j:jul-to-slf4j:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("tools.Application")
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }

    test {
        useJUnitPlatform()
    }
}