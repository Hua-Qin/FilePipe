package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.repository.FileEntry
import dev.bikram.filepipe.data.repository.FileOperationRepository
import dev.bikram.filepipe.domain.model.Rule
import javax.inject.Inject

class PreviewRuleUseCase @Inject constructor(
    private val fileOperationRepository: FileOperationRepository
) {
    suspend operator fun invoke(rule: Rule): List<FileEntry> {
        if (rule.sourceFolderPaths.isEmpty() || rule.fileExtensions.isEmpty()) return emptyList()
        return rule.sourceFolderPaths.flatMap { path ->
            fileOperationRepository.listMatchingFiles(
                folderPath = path,
                extensions = rule.fileExtensions,
                scanSubdirectories = rule.scanSubdirectories
            )
        }
    }
}
