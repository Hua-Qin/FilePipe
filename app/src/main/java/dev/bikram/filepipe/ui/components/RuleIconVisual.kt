package dev.bikram.filepipe.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.ui.graphics.vector.ImageVector
import dev.bikram.filepipe.domain.model.RuleIcon

fun RuleIcon.toImageVector(): ImageVector = when (this) {
    RuleIcon.DEFAULT -> Icons.Filled.FolderSpecial
    RuleIcon.IMAGE -> Icons.Filled.Image
    RuleIcon.SCREENSHOT -> Icons.Filled.Screenshot
    RuleIcon.VIDEO -> Icons.Filled.Movie
    RuleIcon.MUSIC -> Icons.Filled.MusicNote
    RuleIcon.DOWNLOAD -> Icons.Filled.Download
    RuleIcon.DOCUMENT -> Icons.AutoMirrored.Filled.TextSnippet
}
