package dev.bikram.filepipe.domain.model

/** When [RuleTemplate.autoFilesystemSource] is set, [RuleDetailViewModel] pre-fills source folders in all-files mode. */
enum class TemplateAutoSource {
    SCREENSHOTS,
    DOWNLOADS,
}

data class RuleTemplate(
    val name: String,
    val extensions: List<String>,
    val operationMode: OperationMode = OperationMode.MOVE,
    val scanSubdirectories: Boolean = false,
    val suggestedIcon: RuleIcon = RuleIcon.DEFAULT,
    val autoFilesystemSource: TemplateAutoSource? = null,
) {
    companion object {
        val ALL_FILES =
            RuleTemplate(
                name = "All Files",
                extensions = listOf(ALL_FILES_EXTENSION),
                suggestedIcon = RuleIcon.DEFAULT,
            )

        val NO_EXTENSION =
            RuleTemplate(
                name = "No Extension",
                extensions = listOf(NO_EXTENSION_TOKEN),
                suggestedIcon = RuleIcon.DRAFT,
            )

        val CATEGORIES =
            listOf(
                RuleTemplate(
                    name = "Screenshots",
                    extensions = listOf(".png", ".jpg", ".jpeg"),
                    suggestedIcon = RuleIcon.SCREENSHOT,
                    autoFilesystemSource = TemplateAutoSource.SCREENSHOTS,
                ),
                RuleTemplate(
                    name = "Images",
                    extensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".heic", ".webp", ".bmp"),
                    scanSubdirectories = true,
                    suggestedIcon = RuleIcon.IMAGE,
                ),
                RuleTemplate(
                    name = "Documents",
                    extensions = listOf(".pdf", ".docx", ".doc", ".txt", ".odt"),
                    suggestedIcon = RuleIcon.DOCUMENT,
                ),
                RuleTemplate(
                    name = "Downloads",
                    extensions = listOf(".jpg", ".jpeg", ".png", ".mp4", ".pdf"),
                    suggestedIcon = RuleIcon.DOWNLOAD,
                    autoFilesystemSource = TemplateAutoSource.DOWNLOADS,
                ),
                RuleTemplate(
                    name = "Installables",
                    extensions = listOf(".apk", ".apkm", ".xapk", ".zip"),
                    suggestedIcon = RuleIcon.INSTALLABLE,
                ),
                RuleTemplate(
                    name = "Music",
                    extensions = listOf(".mp3", ".flac", ".wav", ".aac", ".m4a", ".ogg", ".opus"),
                    suggestedIcon = RuleIcon.MUSIC,
                ),
                RuleTemplate(
                    name = "Videos",
                    extensions = listOf(".mp4", ".mov", ".mkv", ".avi", ".webm"),
                    suggestedIcon = RuleIcon.VIDEO,
                ),
            )

        val ALL = CATEGORIES + listOf(ALL_FILES, NO_EXTENSION)
    }
}
