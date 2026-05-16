package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.materialSymbolName
import dev.bikram.filepipe.ui.common.FilePipeMaterialRoundedSymbol

/** Curated emoji shortcuts for rule icons (Unicode only, no assets). Twelve presets; custom slot + apply in the rule sheet. */
val RuleIconEmojiPresets: List<String> =
    listOf(
        "\uD83D\uDCC1",
        "\uD83D\uDDBC\uFE0F",
        "\uD83D\uDCF8",
        "\uD83C\uDFAC",
        "\uD83C\uDFB5",
        "\uD83D\uDCBF",
        "\uD83C\uDFA7",
        "\uD83D\uDCE5",
        "\uD83D\uDCC4",
        "\uD83D\uDCDA",
        "\uD83C\uDFAE",
        "\u2B50",
    )

@Composable
fun RuleIconOrEmoji(
    iconEmoji: String?,
    icon: RuleIcon,
    modifier: Modifier = Modifier,
    vectorSize: Dp,
    emojiFontSize: TextUnit,
    tint: Color,
    contentDescription: String? = null,
) {
    val emoji = iconEmoji?.trim()?.takeIf { it.isNotEmpty() }
    val semanticsModifier =
        if (contentDescription == null) {
            modifier.clearAndSetSemantics { }
        } else {
            modifier
        }
    if (emoji != null) {
        Text(
            text = emoji,
            fontSize = emojiFontSize,
            color = tint,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = semanticsModifier,
        )
    } else {
        FilePipeMaterialRoundedSymbol(
            name = icon.materialSymbolName(),
            size = vectorSize,
            contentDescription = contentDescription,
            modifier = semanticsModifier.size(vectorSize),
            tint = tint,
        )
    }
}
