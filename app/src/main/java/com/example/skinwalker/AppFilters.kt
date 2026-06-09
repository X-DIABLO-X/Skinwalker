package com.example.skinwalker

object AppFilters {
    fun matchesQuery(label: String, packageName: String, query: String): Boolean {
        val normalized = query.trim()
        return normalized.isBlank() ||
            label.contains(normalized, ignoreCase = true) ||
            packageName.contains(normalized, ignoreCase = true)
    }
}
