// To avoid caching issues with project accessors in case the project is checked out in different directories.
// Gradle warning when this is not set:
//
//   > Project accessors enabled, but root project name not explicitly set for 'buildSrc'.
//   > Checking out the project in different folders will impact the generated code and
//   > implicitly the buildscript classpath, breaking caching
rootProject.name = "buildSrc"
