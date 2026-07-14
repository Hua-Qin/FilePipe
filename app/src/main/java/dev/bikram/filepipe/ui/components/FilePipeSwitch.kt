package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.bikram.filepipe.ui.common.FilePipeMaterialRoundedSymbol
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound

@Composable
fun FilePipeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val playTap = rememberPlayTapSound()
    Switch(
        checked = checked,
        onCheckedChange =
            if (onCheckedChange != null) {
                {
                    playTap()
                    onCheckedChange(it)
                }
            } else {
                null
            },
        modifier = modifier,
        thumbContent =
            if (checked) {
                {
                    FilePipeMaterialRoundedSymbol(
                        name = "check",
                        contentDescription = null,
                        size = SwitchDefaults.IconSize,
                    )
                }
            } else {
                null
            },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}
