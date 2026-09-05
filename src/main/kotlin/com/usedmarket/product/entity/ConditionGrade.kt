package com.usedmarket.product.entity

/**
 * Overall condition grade of a used product.
 * The detailed 0-100 condition score breakdown lives in a separate
 * ProductConditionScore entity (Batch 2C), this enum is the coarse label
 * shown alongside the score, e.g. "Condition: Very Good - 87/100".
 */
enum class ConditionGrade {
    EXCELLENT,
    VERY_GOOD,
    GOOD,
    FAIR,
    POOR
}
