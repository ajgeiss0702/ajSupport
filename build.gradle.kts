plugins {
    java
    id("com.github.johnrengelman.shadow").version("6.1.0")
}

group = "us.ajg0702"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
//    implementation("net.dv8tion:JDA:5.1.2")
    implementation("io.github.JDA-Fork:JDA:55d824408c")
    implementation("ch.qos.logback:logback-classic:1.4.12")

    implementation("com.google.code.gson:gson:2.8.9")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "us.ajg0702.bots.ajsupport.SupportBot"
    }
}

task<JavaExec>(name = "run") {
    standardInput = System.`in`
    dependsOn("shadowJar")

    mainClass.set("us.ajg0702.bots.ajsupport")
    workingDir = file("./test")
}