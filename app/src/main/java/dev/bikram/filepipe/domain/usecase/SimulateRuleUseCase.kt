package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.repository.FileOperationRepository
import dev.bikram.filepipe.domain.model.PreviewFileResult
import dev.bikram.filepipe.domain.model.Rule
import javax.inject.Inject

class SimulateRuleUseCase @Inject constructor(
    private val fileOperationRepository: FileOperationRepository
) {
    suspend operator fun invoke(rule: Rule): List<PreviewFileResult> {
        if (rule.sourceFolderPaths.isEmpty() || rule.fileExtensions.isEmpty()) return emptyList()

        val fileEntries = rule.sourceFolderPaths.flatMap { path ->
            fileOperationRepository.listMatchingFiles(
                folderUriString = path,
                extensions = rule.fileExtensions,
                scanSubdirectories = rule.scanSubdirectories,
                filenamePattern = rule.filenamePattern,
                minFileSizeBytes = rule.minFileSizeBytes,
                maxFileSizeBytes = rule.maxFileSizeBytes,
                minAgeDays = rule.minAgeDays,
                maxAgeDays = rule.maxAgeDays,
                excludePatterns = rule.excludePatterns
            )
        }

        return fileEntries.map { entry ->
            fileOperationRepository.simulateMove(
                sourceEntry = entry,
                destFolderUriString = rule.destinationFolderPath,
                conflictPolicy = rule.conflictPolicy
            )
        }
    }
}
