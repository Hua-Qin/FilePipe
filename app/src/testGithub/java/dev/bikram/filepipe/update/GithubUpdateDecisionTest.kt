package dev.bikram.filepipe.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdateDecisionTest {
    @Test
    fun sameVersionIsNotAnUpdateEvenWhenApkAssetChanged() {
        assertFalse(
            isGithubReleaseUpdateAvailable(
                remoteVersion = "3.7.5",
                installedVersion = "3.7.5",
                remoteReleaseFingerprint = "3.7.5|2026-05-28T01:00:00Z",
                effectiveAcknowledgedFingerprint = "3.7.5|2026-05-27T01:00:00Z",
            ),
        )
    }

    @Test
    fun newerVersionIsAnUpdateWhenItHasNotBeenAcknowledged() {
        assertTrue(
            isGithubReleaseUpdateAvailable(
                remoteVersion = "3.7.6",
                installedVersion = "3.7.5",
                remoteReleaseFingerprint = "3.7.6|2026-05-28T01:00:00Z",
                effectiveAcknowledgedFingerprint = null,
            ),
        )
    }

    @Test
    fun newerVersionIsNotAnUpdateAfterItHasBeenAcknowledged() {
        assertFalse(
            isGithubReleaseUpdateAvailable(
                remoteVersion = "3.7.6",
                installedVersion = "3.7.5",
                remoteReleaseFingerprint = "3.7.6|2026-05-28T01:00:00Z",
                effectiveAcknowledgedFingerprint = "3.7.6|2026-05-28T01:00:00Z",
            ),
        )
    }

    @Test
    fun stableReleaseIsNewerThanMatchingPreview() {
        assertTrue(isRemoteVersionNewer("v3.9.8", "3.9.8-preview-239"))
        assertFalse(isRemoteVersionNewer("v3.9.8-Preview-239", "3.9.8"))
    }

    @Test
    fun newerPreviewRunIsNewer() {
        assertTrue(isRemoteVersionNewer("v3.9.8-Preview-240", "3.9.8-preview-239"))
    }
}
