package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileExtensionChips(
    extensions: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onUseTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        extensions.forEach { ext ->
            FilePipeInputChip(
                selected = true,
                onClick = {
                    onRemove(ext)
                },
                label = { Text(ext) },
                colors =
                    InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove $ext",
                        modifier = Modifier.size(InputChipDefaults.AvatarSize),
                    )
                },
            )
        }
        FilePipeFilterChip(
            selected = false,
            onClick = {
                showAddDialog = true
            },
            label = { Text("Add type") },
            leadingIcon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add file type",
                    modifier = Modifier.size(InputChipDefaults.AvatarSize),
                )
            },
        )
        FilePipeFilterChip(
            selected = false,
            onClick = onUseTemplate,
            label = { Text("Use template") },
            leadingIcon = {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Use template",
                    modifier = Modifier.size(InputChipDefaults.AvatarSize),
                )
            },
        )
    }

    if (showAddDialog) {
        AddExtensionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { ext ->
                onAdd(ext)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddExtensionDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add file type") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Extension (e.g. .jpg, mp4)") },
                singleLine = true,
                placeholder = { Text(".jpg") },
            )
        },
        confirmButton = {
            FilePipeTextButton(
                onClick = {
                    val ext =
                        text
                            .trim()
                            .let {
                                if (it.startsWith(".")) it else ".$it"
                            }.lowercase()
                    if (ext.length > 1) onAdd(ext)
                },
                enabled = text.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            FilePipeTextButton(onClick = {
                onDismiss()
            }) {
                Text("Cancel")
            }
        },
    )
}
