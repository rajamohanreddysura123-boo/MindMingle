package com.rajamohan.mindmingle.domain.model

enum class PremiumPlan(
    val id: String,
    val label: String,
    val periodLabel: String
) {
    MONTHLY(id = "plus_monthly", label = "MindMingle+ Monthly", periodLabel = "/month"),
    ANNUAL(id = "plus_annual", label = "MindMingle+ Annual", periodLabel = "/year");

    companion object {
        fun fromId(id: String): PremiumPlan? = entries.firstOrNull { it.id == id }
    }
}
