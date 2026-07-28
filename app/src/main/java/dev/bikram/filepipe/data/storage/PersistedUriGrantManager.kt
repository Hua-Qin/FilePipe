package dev.bikram.filepipe.data.storage

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistedUriGrantManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun releaseUnused(
            candidateUris: Collection<String>,
            retainedUris: Collection<String>,
        ) {
            val retained = retainedUris.filterTo(hashSetOf()) { it.startsWith("content://") }
            candidateUris
                .asSequence()
                .filter { it.startsWith("content://") && it !in retained }
                .distinct()
                .forEach { candidateUri ->
                    val uri = candidateUri.toUri()
                    val persistedPermission =
                        context.contentResolver.persistedUriPermissions.firstOrNull { permission ->
                            permission.uri == uri
                        } ?: return@forEach
                    var permissionFlags = 0
                    if (persistedPermission.isReadPermission) {
                        permissionFlags = permissionFlags or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    if (persistedPermission.isWritePermission) {
                        permissionFlags = permissionFlags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    }
                    if (permissionFlags == 0) return@forEach
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(uri, permissionFlags)
                    }.onFailure { error ->
                        DiagnosticLog.record(
                            context,
                            "Failed to release persisted URI permission for $candidateUri",
                            error,
                        )
                    }
                }
        }
    }
