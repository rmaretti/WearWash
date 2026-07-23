package com.wearwash.app.domain.logic

import com.wearwash.app.domain.model.WashingCriteriaType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WashingRule(
    val type: WashingCriteriaType,
    val usageThreshold: Int? = null,
    val dayThreshold: Int? = null,
)

data class WashingReadiness(
    val needsWashing: Boolean,
    val reason: WashingReadinessReason?,
)

enum class WashingReadinessReason {
    Usage,
    Date,
    UsageAndDate,
}

fun evaluateWashingReadiness(
    rule: WashingRule,
    usesSinceWash: Int,
    lastWashingDate: LocalDate?,
    today: LocalDate = LocalDate.now(),
): WashingReadiness {
    val usageReached = rule.usageThreshold?.let { usesSinceWash >= it } == true
    val daysSinceWash = lastWashingDate?.let { ChronoUnit.DAYS.between(it, today).toInt() }
    val dateReached = rule.dayThreshold?.let { threshold ->
        daysSinceWash?.let { it >= threshold } == true
    } == true

    return when (rule.type) {
        WashingCriteriaType.ByUsage -> WashingReadiness(
            usageReached,
            if (usageReached) WashingReadinessReason.Usage else null,
        )
        WashingCriteriaType.ByDate -> WashingReadiness(
            dateReached,
            if (dateReached) WashingReadinessReason.Date else null,
        )
        WashingCriteriaType.ByUsageOrDate -> WashingReadiness(
            usageReached || dateReached,
            when {
                usageReached && dateReached -> WashingReadinessReason.UsageAndDate
                usageReached -> WashingReadinessReason.Usage
                dateReached -> WashingReadinessReason.Date
                else -> null
            },
        )
        WashingCriteriaType.Manual -> WashingReadiness(false, null)
    }
}
