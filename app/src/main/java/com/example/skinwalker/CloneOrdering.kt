package com.example.skinwalker

object CloneOrdering {
    fun sorted(clones: List<CloneEntry>): List<CloneEntry> {
        return clones.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName ?: it.packageName })
    }
}
