package com.example.skinwalker

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun profileDisplaySlotUsesOneBasedLabels() {
        val profile = SkinwalkerProfile(
            id = 1,
            name = "Alex",
            phone = "0987654321",
            createdAt = 42L
        )

        assertEquals("Profile 2", profile.displaySlot)
    }

    @Test
    fun cloneOrderingUsesDisplayNameThenPackageName() {
        val zed = CloneEntry(
            packageName = "com.example.zed",
            displayName = "Zed",
            createdAt = 42L,
            status = CloneStatus.INSTALLED,
            lastMessage = "Installed in managed profile."
        )
        val alpha = CloneEntry(
            packageName = "com.example.alpha",
            displayName = null,
            createdAt = 41L,
            status = CloneStatus.REQUESTED,
            lastMessage = "Requested."
        )

        val sorted = CloneOrdering.sorted(listOf(zed, alpha))

        assertEquals(listOf(alpha, zed), sorted)
    }

    @Test
    fun appSearchMatchesLabelsAndPackages() {
        assertTrue(AppFilters.matchesQuery("Signal", "org.thoughtcrime.securesms", "sig"))
        assertTrue(AppFilters.matchesQuery("Signal", "org.thoughtcrime.securesms", "secure"))
        assertTrue(AppFilters.matchesQuery("Signal", "org.thoughtcrime.securesms", " "))
        assertFalse(AppFilters.matchesQuery("Signal", "org.thoughtcrime.securesms", "maps"))
    }
}
