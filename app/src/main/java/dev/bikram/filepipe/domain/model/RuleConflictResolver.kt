package dev.bikram.filepipe.domain.model

internal fun resolveRenameSuffixName(
    name: String,
    exists: (String) -> Boolean,
): String {
    if (!exists(name)) return name
    val extension = name.substringAfterLast('.', "")
    val baseName = if (extension.isNotEmpty()) name.dropLast(extension.length + 1) else name
    var index = 1
    while (true) {
        val candidate = if (extension.isNotEmpty()) "$baseName($index).$extension" else "$baseName($index)"
        if (!exists(candidate)) return candidate
        index++
    }
}
