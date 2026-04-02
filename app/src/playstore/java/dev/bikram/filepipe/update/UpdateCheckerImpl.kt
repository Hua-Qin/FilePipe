package dev.bikram.filepipe.update

import javax.inject.Inject

class UpdateCheckerImpl @Inject constructor() : UpdateChecker {
    override suspend fun checkForUpdate(): UpdateInfo? = null
}
