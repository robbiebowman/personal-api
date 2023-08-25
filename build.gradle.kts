import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.3"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
}

group = "com.robbiebowman"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
	maven("https://jitpack.io")
}

dependencies {
	// Spring
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Libs
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("com.github.robbiebowman:WordleSolver:v1.2")

	implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.2.0")

	implementation("com.slack.api:slack-api-client:1.28.1")
	implementation("com.slack.api:bolt:1.28.1")

	implementation("com.theokanning.openai-gpt3-java:service:0.11.1")

	implementation("com.azure:azure-security-keyvault-secrets:4.6.0")
	implementation("com.azure:azure-identity:1.8.1")

	implementation("com.github.victools:jsonschema-generator:4.31.1")
	implementation("com.github.victools:jsonschema-module-jackson:4.31.1")


	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
