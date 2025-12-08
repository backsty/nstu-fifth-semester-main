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
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    
    // FlatLaf - Современный Look & Feel
    implementation("com.formdev:flatlaf:3.3")
    implementation("com.formdev:flatlaf-intellij-themes:3.3")
}

application {
    mainClass.set("pw.ns2030.Main")
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dconsole.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Xmx512m"
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:unchecked",
        "-Xlint:deprecation"
    ))
}

tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("console.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
    systemProperty("sun.jnu.encoding", "UTF-8")
    
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        systemProperty("native.encoding", "UTF-8")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "pw.ns2030.Main",
            "Implementation-Title" to "Power System",
            "Implementation-Version" to project.version
        )
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    archiveFileName.set("power-system-${project.version}.jar")
}

tasks.register<JavaExec>("demo") {
    group = "application"
    description = "Запускает систему потребителей энергии"
    mainClass.set("pw.ns2030.Main")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("file.encoding", "UTF-8")
    systemProperty("console.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
}