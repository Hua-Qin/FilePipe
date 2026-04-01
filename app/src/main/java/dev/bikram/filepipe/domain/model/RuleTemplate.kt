package dev.bikram.filepipe.domain.model

data class RuleTemplate(
    val name: String,
    val extensions: List<String>,
    val operationMode: OperationMode = OperationMode.MOVE,
    val scanSubdirectories: Boolean = false,
    val suggestedSourcePaths: List<String> = emptyList(),
    val suggestedIcon: RuleIcon = RuleIcon.DEFAULT
) {
    companion object {
        private const val PRIMARY = "/storage/emulated/0"

        val ALL = listOf(
            RuleTemplate(
                name = "Screenshots",
                extensions = listOf(".png", ".jpg", ".jpeg"),
                suggestedIcon = RuleIcon.SCREENSHOT,
                suggestedSourcePaths = listOf(
                    "$PRIMARY/Pictures/Screenshots",
                    "$PRIMARY/DCIM/Screenshots"
                )
            ),
            RuleTemplate(
                name = "Documents",
                extensions = listOf(".pdf", ".docx", ".doc", ".txt", ".odt"),
                suggestedIcon = RuleIcon.DOCUMENT,
                suggestedSourcePaths = listOf(
                    "$PRIMARY/Documents",
                    "$PRIMARY/Download"
                )
            ),
            RuleTemplate(
                name = "Videos",
                extensions = listOf(".mp4", ".mov", ".mkv", ".avi"),
                suggestedIcon = RuleIcon.VIDEO,
                suggestedSourcePaths = listOf(
                    "$PRIMARY/DCIM",
                    "$PRIMARY/Movies"
                )
            ),
            RuleTemplate(
                name = "Music",
                extensions = listOf(".mp3", ".flac", ".wav", ".aac", ".m4a", ".ogg", ".opus"),
                suggestedIcon = RuleIcon.MUSIC,
                suggestedSourcePaths = listOf(
                    "$PRIMARY/Music",
                    "$PRIMARY/Download"
                )
            ),
            RuleTemplate(
                name = "Downloads",
                extensions = listOf(".jpg", ".jpeg", ".png", ".mp4", ".pdf", ".zip"),
                suggestedIcon = RuleIcon.DOWNLOAD,
                suggestedSourcePaths = listOf("$PRIMARY/Download")
            ),
            RuleTemplate(
                name = "All Images",
                extensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".heic", ".webp", ".bmp"),
                scanSubdirectories = true,
                suggestedIcon = RuleIcon.IMAGE,
                suggestedSourcePaths = listOf(
                    "$PRIMARY/DCIM",
                    "$PRIMARY/Pictures"
                )
            )
        )
    }
}
