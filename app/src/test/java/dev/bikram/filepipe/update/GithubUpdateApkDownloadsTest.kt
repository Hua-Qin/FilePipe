package dev.bikram.filepipe.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdateApkDownloadsTest {
    @Test
    fun sanitizesPathCharactersAndForcesApkExtension() {
        val sanitizedName =
            sanitizeUpdateApkDisplayName(
                "folder/release\\candidate.APK",
                FILEPIPE_UPDATE_APK_CACHE_NAME,
            )

        assertEquals("folder_release_candidate.apk", sanitizedName)
    }

    @Test
    fun removesUnsafeCharactersAndLimitsLength() {
        val sanitizedName =
            sanitizeUpdateApkDisplayName(
                "<>:\"/\\|?*\u0000" + "a".repeat(200),
                FILEPIPE_UPDATE_APK_CACHE_NAME,
            )

        assertFalse(sanitizedName.any { character -> character.isISOControl() })
        assertFalse(sanitizedName.any { character -> character in "<>:\"/\\|?*" })
        assertTrue(sanitizedName.length <= 120)
        assertTrue(sanitizedName.endsWith(".apk"))
    }

    @Test
    fun usesFallbackForBlankOrPunctuationOnlyName() {
        assertEquals(
            FILEPIPE_UPDATE_APK_CACHE_NAME,
            sanitizeUpdateApkDisplayName(" ... ", FILEPIPE_UPDATE_APK_CACHE_NAME),
        )
    }
}
