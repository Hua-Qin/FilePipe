package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
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
    // The checked thumb is painted with onPrimary, so the check has to be drawn in primary to read
    // against it. Material's own default pairs an onPrimary thumb with an onPrimaryContainer icon,
    // which only contrasts under the 2021 color spec; the app generates its scheme with the 2025
    // spec, where onPrimaryContainer is itself dark in dark mode - leaving a near-black check on a
    // near-black thumb. Keep in parity with Remember's RememberSwitch.
    colors: SwitchColors = SwitchDefaults.colors(checkedIconColor = MaterialTheme.colorScheme.primary),
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
