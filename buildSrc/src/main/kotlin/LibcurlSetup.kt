import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

fun KotlinMultiplatformExtension.setupMingwLibcurlFor(targetName: String, project: Project) {

    // Linking the test executable for the Windows target requires the import library for libcurl (libcurl.dll.a).
    // This is true not only on Windows, but also on Linux and macOS when cross-compiling (even without actually
    // running the tests, we can still link the executable on the platform).
    // This import library is checked-in this git repo for this purpose. It was taken from a local msys2 installation
    // (<msys2 home>/mingw64/lib).
    targets.named<KotlinNativeTarget>(targetName) {
        linkDebugLibcurlImportLibrary(project)
    }

    // When actually running the tests on Windows, the linked executable needs the actual libcurl.dll and its
    // dependencies on the PATH (the import library is just a stub defining the declarations available in the dll).
    // Instead of forcing developers to add that to their PATH (which could impact other programs), we can just modify
    // the PATH for the process run by the test task, and add the DLL from some standard place.
    project.tasks.named<KotlinNativeTest>("${targetName}Test").configure {
        ensureLibcurlOnPath()
    }
}

private fun KotlinNativeTarget.linkDebugLibcurlImportLibrary(project: Project) {
    binaries["debugTest"].linkerOpts("-L${project.rootDir.resolve("cygwin-lib")}")
}

private const val MissingLibcurlError = "libcurl.dll not found in PATH, nor under MINGW_HOME nor MSYS_HOME env " +
    "variables. It is necessary for Ktor tests using the Curl engine."

private fun KotlinNativeTest.ensureLibcurlOnPath() {
    val currentPath = System.getenv("PATH")
    if (hasLibcurlDll(currentPath)) {
        return // good, libcurl.dll is already on the PATH
    }
    val libcurlDir = findStandardLibcurlLocation()
    if (libcurlDir != null) {
        environment("PATH", "$currentPath;${libcurlDir.toAbsolutePath().toString()}")
    }

    // we only fail during the actual task execution
    doFirst {
        if (libcurlDir == null) {
            throw GradleException(MissingLibcurlError)
        }
    }
}

private fun hasLibcurlDll(pathEnv: String): Boolean = pathEnv.split(File.pathSeparator).any { Paths.get(it).containsLibcurlDll() }

private fun Path.containsLibcurlDll(): Boolean =
    Files.isDirectory(this) && Files.newDirectoryStream(this, "libcurl*.dll").use { it.any() }

// Windows devs usually already have libcurl in the mingw64 distribution that comes with Git.
// If they don't have it, they can install msys2 and libcurl, and point to it.
private fun findStandardLibcurlLocation(): Path? =
    findLibcurlInEnvVar("MINGW_HOME", "bin")
        ?: findLibcurlInEnvVar("MSYS_HOME", "mingw64/bin", "usr/bin")
        ?: findLibcurlInGit()

private fun findLibcurlInEnvVar(envVarName: String, vararg relativePathToDlls: String): Path? {
    val envValue = System.getenv(envVarName)?.takeIf { it.isNotBlank() } ?: return null
    val envValuePath = Paths.get(envValue)
    if (Files.notExists(envValuePath)) {
        throw GradleException("The path in %$envVarName% doesn't exist: $envValuePath")
    }
    if (!Files.isDirectory(envValuePath)) {
        throw GradleException("The path in %$envVarName% doesn't point to a directory: $envValuePath")
    }
    return relativePathToDlls
        .map { envValuePath.resolve(it) }
        .firstOrNull { it.containsLibcurlDll() }
}

private fun findLibcurlInGit(): Path? {
    val dllPath = Paths.get("C:\\Program Files\\Git\\mingw64\\bin")
    if (!dllPath.containsLibcurlDll()) {
        return null
    }
    return dllPath
}
