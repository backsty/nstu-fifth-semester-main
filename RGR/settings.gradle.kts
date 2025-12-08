rootProject.name = "RGR"

// Включаем Gradle build scan для аналитики
plugins {
    id("com.gradle.enterprise") version "3.15" apply false
}

// Настройки для зависимостей
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Резервный репозиторий
        maven("https://repo1.maven.org/maven2/")
    }
}