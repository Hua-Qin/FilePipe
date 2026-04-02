package dev.bikram.filepipe.update

interface UpdateChecker {
    suspend fun checkForUpdate(): UpdateInfo?
}

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)
