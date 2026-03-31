package dev.bikram.filepipe.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.export.buildRulesBackupJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExportRulesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository
) {
    suspend fun exportRulesToTreeUri(treeUriString: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (treeUriString.isBlank()) return@withContext Result.failure(IllegalStateException("No export folder"))
        val treeUri = Uri.parse(treeUriString)
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext Result.failure(IllegalStateException("Invalid folder"))
        val rules = ruleRepository.getAllRules().first()
        val json = buildRulesBackupJson(rules)
        val existing = root.findFile(EXPORT_FILE_NAME)
        existing?.delete()
        val file = root.createFile("application/json", EXPORT_FILE_NAME)
            ?: return@withContext Result.failure(IllegalStateException("Could not create file"))
        runCatching {
            context.contentResolver.openOutputStream(file.uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open output stream")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    companion object {
        const val EXPORT_FILE_NAME = "media_organizer_rules.json"
    }
}
