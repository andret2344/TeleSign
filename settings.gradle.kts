pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
	}
}

rootProject.name = "TeleSign"
