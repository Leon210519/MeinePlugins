plugins { java }

group = "com.specialitems"
version = "1.2.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.placeholderapi:placeholderapi:2.11.6")
    compileOnly("com.github.LoneDev6:api:3.6.1")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}
