package dev.bikram.filepipe.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.MainActivity
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.Rule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutsManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        companion object {
            const val EXTRA_SHORTCUT_RULE_ID = "extra_shortcut_rule_id"
            private const val MAX_SHORTCUTS = 4
        }

        fun updateShortcuts(rules: List<Rule>) {
            val topRules = rules.filter { it.isEnabled }.take(MAX_SHORTCUTS)
            val shortcuts =
                topRules.map { rule ->
                    val intent =
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(EXTRA_SHORTCUT_RULE_ID, rule.id)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                    ShortcutInfoCompat
                        .Builder(context, "rule_${rule.id}")
                        .setShortLabel(rule.name.take(25))
                        .setLongLabel("Run: ${rule.name}")
                        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                        .setIntent(intent)
                        .build()
                }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }
