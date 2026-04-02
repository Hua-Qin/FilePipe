package dev.bikram.filepipe.ui.screens.historydetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.RunStatus
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.ui.components.StatusChip
import dev.bikram.filepipe.ui.components.displayPath
import dev.bikram.filepipe.ui.components.formatTime
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import dev.bikram.filepipe.ui.theme.LocalUseGradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    val playTap = rememberPlayTapSound()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        val msg = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearUserMessage()
    }

    Scaffold(
        containerColor = if (LocalUseGradientBackground.current) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(history?.ruleName ?: "History Detail") },
                navigationIcon = {
                    IconButton(onClick = {
                        playTap()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            history?.let { h ->
                item {
                    RunSummaryCard(
                        history = h,
                        onUndo = {
                            playTap()
                            viewModel.undoRun()
                        }
                    )
                }
                if (files.isNotEmpty()) {
                    item {
                        Text(
                            "Files (${files.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(files, key = { it.id }) { file ->
                        FileMovedCard(file, modifier = Modifier.animateItem())
                    }
                } else {
                    item {
                        Text(
                            "No file records for this run.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunSummaryCard(history: RunHistory, onUndo: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Run Summary", style = MaterialTheme.typography.titleMedium)
                StatusChip(
                    status = history.status,
                    noChanges = history.status == RunStatus.SUCCESS &&
                        history.totalFilesMoved == 0 && history.totalFilesFailed == 0
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Trigger", when (history.triggeredBy) {
                TriggerType.MANUAL -> "Manual"
                TriggerType.SCHEDULED -> "Scheduled"
            })
            SummaryRow("Started", formatTime(history.startedAt))
            history.completedAt?.let { completed ->
                SummaryRow("Completed", formatTime(completed))
                val durationSec = (completed - history.startedAt) / 1000
                SummaryRow("Duration", "${durationSec}s")
            }
            SummaryRow("Files moved", history.totalFilesMoved.toString())
            if (history.totalFilesFailed > 0) {
                SummaryRow("Failed", history.totalFilesFailed.toString())
            }
            history.errorMessage?.let { msg ->
                SummaryRow("Error", msg)
            }
            if (history.isReversed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "This run has been undone.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else if (history.totalFilesMoved > 0 && history.status == RunStatus.SUCCESS) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onUndo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                    Text("  Undo (restore ${history.totalFilesMoved} file(s))")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileMovedCard(file: FileMoved, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isSuccess = file.success && !file.skipped
    val iconColor = when {
        file.skipped -> MaterialTheme.colorScheme.outline
        file.success -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val containerColor = when {
        file.skipped -> MaterialTheme.colorScheme.surfaceContainerLow
        !file.success -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSuccess) Modifier.clickable {
                    openFileWithDefaultApp(context, file.destinationUri)
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File type icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = fileTypeIcon(file.fileName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        file.fileName,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                    )
                }

                Text(
                    "From: ${displayPath(file.sourceUri)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSuccess) {
                    Text(
                        "To: ${displayPath(file.destinationUri)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeKb = file.fileSizeBytes / 1024
                    Text(
                        if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (file.skipped) {
                        Text(
                            "skipped",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    file.errorMessage?.let { err ->
                        Text(
                            err,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun openFileWithDefaultApp(context: Context, uriString: String) {
    if (uriString.isBlank()) {
        Toast.makeText(context, "File location not available", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = Uri.parse(uriString)
    val fileName = uriString.substringAfterLast('/')
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
    }
}

private fun fileTypeIcon(fileName: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "heic", "webp", "bmp", "svg" -> Icons.Default.Image
        "mp4", "mkv", "avi", "mov", "m4v", "webm" -> Icons.Default.Movie
        "mp3", "flac", "aac", "ogg", "m4a", "wav" -> Icons.Default.AudioFile
        "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "odt" -> Icons.Default.Description
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Archive
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
