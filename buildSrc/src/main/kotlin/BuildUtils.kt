import org.gradle.api.Project
import java.net.URL
import java.nio.file.Path

fun Project.relativeDokkaUrl(projectName: String): URL {
    return relativeDokkaPath(projectName).toUri().toURL()
}

fun Project.relativeDokkaPackageListUrl(projectName: String): URL {
    return relativeDokkaPath(projectName).resolve("package-list").toUri().toURL()
}

private fun Project.relativeDokkaPath(projectName: String): Path {
    val buildDirPath = project(":$projectName").buildDir.toPath()
    return buildDirPath.resolve("dokka/$projectName")
}
