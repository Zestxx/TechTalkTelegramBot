plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.tech.talk.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.pengrad:java-telegram-bot-api:7.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
