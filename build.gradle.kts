import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.2.3"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm") version "1.9.22"
	kotlin("plugin.spring") version "1.9.22"
}

group = "com.robbiebowman"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven("https://jitpack.io")
}

dependencies {
	// Spring
	implementation("org.springframework.boot:spring-boot-starter-web:3.3.0")

	// Libs
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("com.github.robbiebowman:WordleSolver:v1.2")
	implementation("com.github.robbiebowman:gpt-tools-annotations:0.0.3")
	implementation("com.github.robbiebowman:mini-crossword-maker:1.1.1")
	implementation("com.robbiebowman:claude-sdk:0.0.3")

	implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.2.0")

	implementation("com.slack.api:slack-api-client:1.28.1")
	implementation("com.slack.api:bolt:1.28.1")

	implementation("com.theokanning.openai-gpt3-java:service:0.18.2")

	implementation("com.azure:azure-security-keyvault-secrets:4.6.0")
	implementation("com.azure:azure-storage-blob:12.26.0")
	implementation("com.azure:azure-identity:1.8.1")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.0")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
