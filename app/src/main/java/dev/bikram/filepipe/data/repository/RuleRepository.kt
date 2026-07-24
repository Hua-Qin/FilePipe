package dev.bikram.filepipe.data.repository

import dev.bikram.filepipe.data.local.dao.RuleDao
import dev.bikram.filepipe.data.local.entity.toDomain
import dev.bikram.filepipe.data.local.entity.toEntity
import dev.bikram.filepipe.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository
    @Inject
    constructor(
        private val ruleDao: RuleDao,
    ) {
        companion object {
            const val TRASH_RETENTION_MILLIS: Long = 30L * 24L * 60L * 60L * 1000L
        }

        fun getAllRules(): Flow<List<Rule>> = ruleDao.getAllRules().map { it.map { entity -> entity.toDomain() } }

        fun getTrashedRules(): Flow<List<Rule>> = ruleDao.getTrashedRules().map { it.map { entity -> entity.toDomain() } }

        suspend fun getRuleById(id: Long): Rule? = ruleDao.getRuleById(id)?.toDomain()

        suspend fun getRuleByIdIncludingTrashed(id: Long): Rule? = ruleDao.getRuleByIdIncludingTrashed(id)?.toDomain()

        suspend fun getAllRulesOrderedBySortOrder(): List<Rule> = ruleDao.getAllRulesOrderedBySortOrder().map { entity -> entity.toDomain() }

        suspend fun getEnabledRules(): List<Rule> = ruleDao.getEnabledRules().map { it.toDomain() }

        /**
         * Inserts a new rule, or updates an existing one **without writing the columns the rule
         * editor doesn't own**. The editor builds its [Rule] from the form fields alone, so every
         * column it doesn't show comes back as a default value: a full-row write would snap the
         * card's expand/collapse override back to the list default, erase the last-run time behind
         * "sort by last run", reset the creation date to now, and un-trash a trashed rule. Those
         * columns are owned by the list and by run bookkeeping - which have their own setters -
         * so they are carried over from the stored row here.
         */
        suspend fun saveRule(rule: Rule): Long {
            if (rule.id == 0L) {
                val nextOrder = ruleDao.getMaxSortOrder() + 1
                return ruleDao.upsertRule(rule.copy(sortOrder = nextOrder).toEntity())
            }
            val edited = rule.toEntity()
            val stored = ruleDao.getRuleByIdIncludingTrashed(rule.id)
            return ruleDao.upsertRule(
                if (stored == null) {
                    edited
                } else {
                    edited.copy(
                        cardModeOverride = stored.cardModeOverride,
                        lastRunStartedAt = stored.lastRunStartedAt,
                        createdAt = stored.createdAt,
                        trashedAt = stored.trashedAt,
                    )
                },
            )
        }

        suspend fun updateRule(rule: Rule) = ruleDao.updateRule(rule.toEntity())

        /** One transaction so observers emit once; preserves drag order until sort mode updates. */
        suspend fun persistOrderedSortIndices(ordered: List<Rule>) {
            val entities =
                ordered.mapIndexed { index, rule ->
                    rule.copy(sortOrder = index).toEntity()
                }
            ruleDao.updateRulesSortOrders(entities)
        }

        /**
         * Records that [ruleId] started a run at [startedAt]. Called for every run, including ones
         * that match no files, so the rules list can sort by last run without depending on history
         * rows (which are prunable and user-deletable).
         */
        suspend fun markRuleRan(
            ruleId: Long,
            startedAt: Long,
        ) = ruleDao.updateLastRunStartedAt(ruleId, startedAt)

        suspend fun moveRuleToTrash(ruleId: Long) = ruleDao.moveRuleToTrash(ruleId, System.currentTimeMillis())

        suspend fun restoreRuleFromTrash(ruleId: Long) = ruleDao.restoreRuleFromTrash(ruleId, System.currentTimeMillis())

        suspend fun updateCardModeOverride(
            ruleId: Long,
            override: Boolean,
        ) = ruleDao.updateCardModeOverride(ruleId, override)

        suspend fun clearCardModeOverrides() = ruleDao.clearCardModeOverrides()

        suspend fun deleteRuleForever(ruleId: Long) = ruleDao.deleteRuleById(ruleId)

        suspend fun autoEmptyTrashOlderThan(cutoffMillis: Long) = ruleDao.deleteTrashedRulesOlderThan(cutoffMillis)

        suspend fun emptyTrashForever() = ruleDao.deleteAllTrashedRules()

        suspend fun deleteRule(ruleId: Long) = deleteRuleForever(ruleId)

        suspend fun getAllRuleIds(): List<Long> = ruleDao.getAllRuleIds()

        suspend fun deleteAllRules() = ruleDao.deleteAllRules()

        suspend fun replaceAllRules(rules: List<Rule>) {
            ruleDao.deleteAllRules()
            rules.forEachIndexed { index, rule ->
                ruleDao.upsertRule(rule.copy(id = 0L, sortOrder = index).toEntity())
            }
        }
    }
