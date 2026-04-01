package dev.bikram.filepipe.domain.model

enum class RuleIcon {
    DEFAULT,
    IMAGE,
    SCREENSHOT,
    VIDEO,
    MUSIC,
    DOWNLOAD,
    DOCUMENT;

    companion object {
        fun fromStored(key: String?): RuleIcon =
            entries.find { it.name == key } ?: DEFAULT
    }
}
