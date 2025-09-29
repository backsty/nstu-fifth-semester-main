plugins {
    id("java")
    id("application")
}

group = "pw.ns2030"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Swing Look&Feel для современного интерфейса
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("com.formdev:flatlaf-extras:3.2.5")
    
    // Тестирование
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "pw.ns2030.Main"
    // Устанавливаем правильную кодировку для запуска
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dconsole.encoding=UTF-8",
        "-Duser.language=ru",
        "-Duser.country=RU"
    )
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.test {
    useJUnitPlatform()
}

// Устанавливаем UTF-8 для компиляции
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "pw.ns2030.Main"
        )
    }
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

// Настройки запуска с правильной кодировкой
tasks.run.configure {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("console.encoding", "UTF-8")
    systemProperty("user.language", "ru")
    systemProperty("user.country", "RU")
}