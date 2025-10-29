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
    // Тестирование
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core:5.5.0")
    
    // Для JSON валидации (опционально)
    testImplementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
}

application {
    mainClass = "pw.ns2030.Main"
    // ИСПРАВЛЕНИЕ: Улучшенные настройки кодировки
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dconsole.encoding=UTF-8",
        "-Dstdout.encoding=UTF-8", 
        "-Dstderr.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Duser.language=ru",
        "-Duser.country=RU",
        "-Xmx512m"
    )
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
    maxHeapSize = "256m"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:unchecked",
        "-Xlint:deprecation"
        // Убрали "-Dfile.encoding=UTF-8" - это не флаг компилятора!
    ))
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "pw.ns2030.Main",
            "Implementation-Title" to "JSON Serializer with Reflection",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "pw.ns2030"
        )
    }
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    
    archiveFileName.set("json-serializer-${project.version}.jar")
}

tasks.run.configure {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("console.encoding", "UTF-8")
    systemProperty("stdout.encoding", "UTF-8")
    systemProperty("stderr.encoding", "UTF-8")
    systemProperty("sun.jnu.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
    systemProperty("native.encoding", "UTF-8")
    systemProperty("user.language", "ru")
    systemProperty("user.country", "RU")
    
    // Для Windows консоли
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        systemProperty("sun.stdout.encoding", "UTF-8")
        systemProperty("sun.stderr.encoding", "UTF-8")
    }
    
    systemProperty("json.serializer.debug", "true")
    jvmArgs(
        "-Xss2m",
        "-Dfile.encoding=UTF-8",
        "-Dconsole.encoding=UTF-8", 
        "-Dsun.jnu.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-Djava.awt.headless=true",
        // Принудительная UTF-8 для всех потоков
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8"
    )

    systemProperty("json.serializer.debug", "true")
}

tasks.register("generateDocs") {
    group = "documentation"
    description = "Генерирует документацию по API"
    
    doLast {
        println("Генерация документации...")
        println("Основные классы:")
        println("- JsonSerializer: основной сериализатор")
        println("- JsonDeserializer: десериализатор")
        println("- ReferenceTracker: отслеживание ссылок")
        println("- Аннотации: JsonSerializable, JsonField, JsonIgnore")
    }
}

tasks.register("demo") {
    group = "application"
    description = "Запускает демонстрацию JSON-сериализатора"
    dependsOn("classes")
    
    doLast {
        javaexec {
            mainClass.set("pw.ns2030.Main")
            classpath = sourceSets["main"].runtimeClasspath
            systemProperty("file.encoding", "UTF-8")
            systemProperty("console.encoding", "UTF-8")
        }
    }
}

tasks.register("checkReflection") {
    group = "verification"
    description = "Проверяет доступность рефлексии"
    dependsOn("classes")
    
    doLast {
        println("Проверка рефлексии...")
        println("Java version: ${System.getProperty("java.version")}")
        println("Reflection API доступен: ${Class.forName("java.lang.reflect.Method") != null}")
        println("Annotations API доступен: ${Class.forName("java.lang.annotation.Annotation") != null}")
    }
}

tasks.register("ideSetup") {
    group = "ide"
    description = "Настраивает проект для IDE"
    
    doLast {
        println("Проект настроен для разработки")
        println("Используйте: ./gradlew run для запуска")
        println("Используйте: ./gradlew test для тестов")
        println("Используйте: ./gradlew demo для демонстрации")
    }
}