package dev.bikram.filepipe.domain.model

data class RuleTemplate(
    val name: String,
    val extensions: List<String>,
    val operationMode: OperationMode = OperationMode.MOVE,
    val scanSubdirectories: Boolean = false,
    val suggestedIcon: RuleIcon = RuleIcon.DEFAULT
) {
    companion object {
        val ALL = listOf(
            RuleTemplate(
                name = "Screenshots",
                extensions = listOf(".png", ".jpg", ".jpeg"),
                suggestedIcon = RuleIcon.SCREENSHOT
            ),
            RuleTemplate(
                name = "Documents",
                extensions = listOf(".pdf", ".docx", ".doc", ".txt", ".odt"),
                suggestedIcon = RuleIcon.DOCUMENT
            ),
            RuleTemplate(
                name = "Videos",
                extensions = listOf(".mp4", ".mov", ".mkv", ".avi"),
                suggestedIcon = RuleIcon.VIDEO
            ),
            RuleTemplate(
                name = "Music",
                extensions = listOf(".mp3", ".flac", ".wav", ".aac", ".m4a", ".ogg", ".opus"),
                suggestedIcon = RuleIcon.MUSIC
            ),
            RuleTemplate(
                name = "Downloads",
                extensions = listOf(".jpg", ".jpeg", ".png", ".mp4", ".pdf", ".zip"),
                suggestedIcon = RuleIcon.DOWNLOAD
            ),
            RuleTemplate(
                name = "All Images",
                extensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".heic", ".webp", ".bmp"),
                scanSubdirectories = true,
                suggestedIcon = RuleIcon.IMAGE
            )
        )
    }
}
