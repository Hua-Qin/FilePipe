package dev.bikram.filepipe.domain.model

enum class RuleIcon {
    DEFAULT,
    IMAGE,
    SCREENSHOT,
    VIDEO,
    MUSIC,
    DOWNLOAD,
    DOCUMENT,
    INSTALLABLE,
    ;

    companion object {
        fun fromStored(key: String?): RuleIcon = entries.find { it.name == key } ?: DEFAULT
    }
}

fun RuleIcon.materialSymbolName(): String =
    when (this) {
        RuleIcon.DEFAULT -> "folder_special"
        RuleIcon.IMAGE -> "image"
        RuleIcon.SCREENSHOT -> "screenshot"
        RuleIcon.VIDEO -> "movie"
        RuleIcon.MUSIC -> "music_note"
        RuleIcon.DOWNLOAD -> "download"
        RuleIcon.DOCUMENT -> "text_snippet"
        RuleIcon.INSTALLABLE -> "android"
    }
