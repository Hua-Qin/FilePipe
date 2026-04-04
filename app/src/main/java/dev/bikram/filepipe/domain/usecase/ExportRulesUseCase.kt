package dev.bikram.filepipe.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.export.buildAppBackupJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportRulesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val runHistoryRepository: RunHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun exportRulesToTreeUri(folderPath: String): Result<String> = withContext(Dispatchers.IO) {
        if (folderPath.isBlank()) return@withContext Result.failure(IllegalStateException("No export folder"))

        val rules = ruleRepository.getAllRules().first()
        val allHistory = runHistoryRepository.getAllHistoryOnce()
        val historyWithFiles = allHistory.map { run ->
            run to runHistoryRepository.getFilesForRunOnce(run.id)
        }
        val settings = userPreferencesRepository.getPreferencesSnapshot()

        val json = buildAppBackupJson(rules, historyWithFiles, settings)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "filepipe_backup_$stamp.json"

        if (folderPath.startsWith("content://")) {
            writeToContentUri(folderPath, fileName, json).map { fileName }
        } else {
            writeToFilePath(folderPath, fileName, json).map { fileName }
        }
    }

    /**
     * Writes the same backup JSON as [exportRulesToTreeUri] to a URI from [androidx.activity.result.contract.ActivityResultContracts.CreateDocument].
     */
    suspend fun exportBackupJsonToDocumentUri(targetUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val rules = ruleRepository.getAllRules().first()
        val allHistory = runHistoryRepository.getAllHistoryOnce()
        val historyWithFiles = allHistory.map { run ->
            run to runHistoryRepository.getFilesForRunOnce(run.id)
        }
        val settings = userPreferencesRepository.getPreferencesSnapshot()
        val json = buildAppBackupJson(rules, historyWithFiles, settings)
        runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Failed to open output stream for export")
            friendlyFileNameFromDocumentUri(targetUri)
        }.fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it) })
    }

    /**
     * [Uri.getLastPathSegment] for SAF document URIs is the full document id (e.g. `primary:Download/foo.json`).
     * For snackbars we only want the leaf file name (e.g. `foo.json`).
     */
    private fun friendlyFileNameFromDocumentUri(documentUri: Uri): String {
        val segment = documentUri.lastPathSegment ?: return "filepipe_backup.json"
        val decoded = Uri.decode(segment)
        val lastSlash = decoded.lastIndexOf('/')
        return if (lastSlash >= 0) {
            decoded.substring(lastSlash + 1)
        } else {
            val lastColon = decoded.lastIndexOf(':')
            if (lastColon >= 0) decoded.substring(lastColon + 1) else decoded
        }
    }

    private fun writeToContentUri(folderUriString: String, fileName: String, json: String): Result<Unit> {
        val treeUri = Uri.parse(folderUriString)
        return runCatching {
            val docTreeUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            val docUri = DocumentsContract.createDocument(
                context.contentResolver,
                docTreeUri,
                "application/json",
                fileName
            ) ?: throw IOException("Failed to create document in cloud folder")
            context.contentResolver.openOutputStream(docUri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Failed to open output stream for cloud document")
        }.fold(onSuccess = { Result.success(Unit) }, onFailure = { Result.failure(it) })
    }

    private fun writeToFilePath(folderPath: String, fileName: String, json: String): Result<Unit> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.canWrite()) {
            return Result.failure(IllegalStateException("Export folder not accessible: $folderPath"))
        }
        return runCatching {
            File(folder, fileName).writeText(json, Charsets.UTF_8)
        }.fold(onSuccess = { Result.success(Unit) }, onFailure = { Result.failure(it) })
    }
}
