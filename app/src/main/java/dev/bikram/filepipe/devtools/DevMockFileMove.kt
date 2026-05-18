package dev.bikram.filepipe.devtools

import dev.bikram.filepipe.domain.model.Rule

object DevMockFileMove {
    const val SOURCE_FOLDER_URI = "file:///storage/emulated/0/FilePipe Mock/Incoming"
    const val DESTINATION_FOLDER_URI = "file:///storage/emulated/0/FilePipe Mock/Organized"
    const val FILE_SIZE_BYTES = 104_857_600L

    fun isMockRule(rule: Rule): Boolean =
        rule.sourceFolderPaths == listOf(SOURCE_FOLDER_URI) &&
            rule.destinationFolderPath == DESTINATION_FOLDER_URI

    fun isMockMovedFile(
        sourceUri: String,
        destinationUri: String,
    ): Boolean = sourceUri.startsWith(SOURCE_FOLDER_URI) && destinationUri.startsWith(DESTINATION_FOLDER_URI)

    fun sourceUri(fileName: String): String = "$SOURCE_FOLDER_URI/$fileName"

    fun destinationUri(fileName: String): String = "$DESTINATION_FOLDER_URI/$fileName"
}
