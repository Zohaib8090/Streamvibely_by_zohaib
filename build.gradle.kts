plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Dependabot alert: Ktor's npm package pulls in `ws` < 8.21.0 (memory exhaustion DoS). Force the patched version.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension>().resolution("ws", "8.21.0")
}
