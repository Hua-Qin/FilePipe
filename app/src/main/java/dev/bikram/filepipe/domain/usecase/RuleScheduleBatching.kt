package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleSchedule

internal data class CoalescedRuleScheduleGroup(
    val schedule: RuleSchedule,
    val ruleIds: List<Long>,
)

internal fun coalescedRuleScheduleGroups(rules: List<Rule>): List<CoalescedRuleScheduleGroup> =
    rules
        .filter { rule -> rule.isEnabled && rule.schedule != null }
        .groupBy { rule -> scheduleKey(rule.schedule!!) }
        .values
        .map { group ->
            CoalescedRuleScheduleGroup(
                schedule = group.first().schedule!!,
                ruleIds = group.map { rule -> rule.id },
            )
        }

internal fun batchTagForRuleIds(ruleIds: LongArray): String = "batch_${ruleIds.sorted().joinToString("_")}"

internal fun scheduleKey(schedule: RuleSchedule): String = "${schedule.type}_${schedule.hour}_${schedule.minute}_${schedule.dayOfWeek}_${schedule.intervalHours}"
