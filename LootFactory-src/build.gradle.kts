plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    // ⬇️ NEU: offizielles PlaceholderAPI Repo
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("me.clip:placeholderapi:2.11.6") // <— so ist’s richtig
    compileOnly(files("../LootPets/build/libs/LootPets-1.0.0.jar"))
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // falls du später Variablen ins plugin.yml expandieren willst
}

tasks.jar {
    archiveFileName.set("LootFactory.jar")
    // Keine "from(...)"-Angaben hier! Gradle packt src/main/resources automatisch mit ein.
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}
