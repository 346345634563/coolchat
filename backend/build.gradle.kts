plugins {
    java
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.google.cloud.tools.jib") version "3.4.4"
}

group = "com.inf5190"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }
}

jib {
    to {
        image = "northamerica-northeast1-docker.pkg.dev/app-chat-a292d/inf-5190/chat-app"
        auth {
            username = "_json_key"
            password = file("src/main/resources/firebase-production-key.json").readText(Charsets.UTF_8)
        }
    }
}
