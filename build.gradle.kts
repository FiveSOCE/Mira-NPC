plugins {
    java
}

group = "com.mira"
version = "0.1.6"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApiVersion = providers.gradleProperty("paperApiVersion").orElse("1.21.11-R0.1-SNAPSHOT")
val compileJavaVersion = providers.gradleProperty("compileJavaVersion").map(String::toInt).orElse(21)
val bytecodeJavaVersion = providers.gradleProperty("bytecodeJavaVersion").map(String::toInt).orElse(21)

dependencies {
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion.get()}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(compileJavaVersion.get()))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveFileName.set("MiraNPC-${project.version}.jar")
}
