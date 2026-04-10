// Top-level build file
plugins {
    id("com.android.application") version "8.9.0" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
