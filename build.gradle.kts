plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.tech.talk.bot"
version = "0.1"

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

// Add a task to create a fat JAR that includes the Main-Class attribute in the manifest
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.example.telegrambot.TechTalkBotKt"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
