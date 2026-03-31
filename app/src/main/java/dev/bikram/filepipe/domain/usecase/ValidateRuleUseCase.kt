package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.Rule
import javax.inject.Inject

class ValidateRuleUseCase @Inject constructor() {
    sealed class Result {
        data object Valid : Result()
        data class Invalid(val errors: List<String>) : Result()
    }

    operator fun invoke(rule: Rule): Result {
        val errors = buildList {
            if (rule.name.isBlank()) add("Rule name is required")
            if (rule.sourceFolderUris.isEmpty()) add("At least one source folder is required")
            if (rule.destinationFolderUri.isBlank()) add("Destination folder is required")
            if (rule.fileExtensions.isEmpty()) add("At least one file type is required")
            rule.schedule?.let { schedule ->
                if (schedule.hour !in 0..23) add("Invalid hour in schedule")
                if (schedule.minute !in 0..59) add("Invalid minute in schedule")
            }
        }
        return if (errors.isEmpty()) Result.Valid else Result.Invalid(errors)
    }
}
