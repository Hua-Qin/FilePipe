package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.repository.FileEntry
import dev.bikram.filepipe.data.repository.FileOperationRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.OperationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class UndoResult(
    val totalReversed: Int,
    val totalFailed: Int,
    val errors: List<String>
)

class UndoRunUseCase @Inject constructor(
    private val runHistoryRepository: RunHistoryRepository,
    private val fileOperationRepository: FileOperationRepository
) {
    suspend operator fun invoke(historyId: Long): UndoResult = withContext(Dispatchers.IO) {
        val history = runHistoryRepository.getHistoryById(historyId)
            ?: return@withContext UndoResult(0, 0, listOf("Run not found"))

        if (history.isReversed) {
            return@withContext UndoResult(0, 0, listOf("This run has already been undone"))
        }

        val movedFiles = runHistoryRepository.getFilesForRunOnce(historyId)
            .filter { it.success && !it.skipped && it.destinationUri.isNotBlank() }

        var reversed = 0
        var failed = 0
        val errors = mutableListOf<String>()

        movedFiles.forEach { fileMoved ->
            val destFile = File(fileMoved.destinationUri)
            val sourceFolder = File(fileMoved.sourceUri).parentFile

            if (!destFile.exists()) {
                errors.add("${fileMoved.fileName}: destination file no longer exists")
                failed++
                return@forEach
            }
            if (sourceFolder == null || !sourceFolder.exists() || !sourceFolder.canWrite()) {
                errors.add("${fileMoved.fileName}: source folder not accessible")
                failed++
                return@forEach
            }

            val reverseResult = fileOperationRepository.moveFile(
                sourceEntry = FileEntry(destFile),
                destFolderPath = sourceFolder.absolutePath,
                conflictPolicy = ConflictPolicy.RENAME_SUFFIX,
                operationMode = OperationMode.MOVE
            )

            if (reverseResult.success) {
                reversed++
            } else {
                failed++
                reverseResult.errorMessage?.let { errors.add("${fileMoved.fileName}: $it") }
            }
        }

        if (reversed > 0 || movedFiles.isEmpty()) {
            runHistoryRepository.markRunReversed(historyId)
        }

        UndoResult(reversed, failed, errors)
    }
}
