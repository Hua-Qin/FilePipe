package dev.bikram.filepipe.domain.usecase

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesBackupDirtyTracker
    @Inject
    constructor() {
        private val hasPendingChangeSinceLastTreeExport = AtomicBoolean(false)

        fun markRulesChangedSinceLastTreeExport() {
            hasPendingChangeSinceLastTreeExport.set(true)
        }

        fun consumePendingChangeSinceLastTreeExport(): Boolean = hasPendingChangeSinceLastTreeExport.getAndSet(false)
    }
