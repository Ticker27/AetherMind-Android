package com.aether.renderer

data class BuildMatrixTarget(
    val name: String,
    val compileSdk: Int,
    val ndkVersion: String,
    val abi: String
)

object BuildMatrix {
    val primary: BuildMatrixTarget = BuildMatrixTarget(
        name = "primary-arm64",
        compileSdk = 35,
        ndkVersion = "26.3.11579264",
        abi = "arm64-v8a"
    )

    const val debugArtifact: String = "AT95_qa_debug.apk"
    const val releaseArtifact: String = "AT95_qa_release.apk"
}
