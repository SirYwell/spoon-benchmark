plugins {
    java
    id("me.champeau.jmh") version "0.6.5"
}

group = "de.sirywell"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("fr.inria.gforge.spoon:spoon-core:9.1.0-beta-21")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}