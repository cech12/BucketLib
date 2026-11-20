package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.api.BucketLib;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Set;

/**
 * Marks tests that are known to fail on some loaders because of a real bug ("deactivated" tests). On these loaders the
 * test is still executed: it passes while the issue is present and FAILS as soon as the issue is gone, so the marker
 * cannot be forgotten after a fix. On all other loaders the test runs normally.
 * <p>
 * Every known issue has an id ({@code KI-<n>}) and is listed in TESTING.md ("Known issues"). When the bug shows up as
 * an exception, wrap the call in {@link TestUtils#noException}, so that it counts as failed assertion of the bug and
 * not as broken test code.
 */
public final class KnownIssues {

    private KnownIssues() {}

    /**
     * Runs a synchronous test body and succeeds the test. On the affected platforms every failed assertion of the body
     * counts as "issue still present", so the body should only contain the assertion of the issue itself. Setup and
     * plausibility checks belong before this call.
     *
     * @param helper game test helper
     * @param issue id and short description of the known issue, see TESTING.md
     * @param affectedPlatforms platforms where the issue is known (see {@link Platforms})
     * @param test test body with assertions
     */
    public static void run(GameTestHelper helper, String issue, Set<String> affectedPlatforms, Runnable test) {
        check(helper, issue, affectedPlatforms, test);
        helper.succeed();
    }

    /**
     * Like {@link #run}, but does not succeed the test, so that further assertions (or other known issues) can follow.
     *
     * @param helper game test helper
     * @param issue id and short description of the known issue, see TESTING.md
     * @param affectedPlatforms platforms where the issue is known (see {@link Platforms})
     * @param test test body with assertions
     */
    public static void check(GameTestHelper helper, String issue, Set<String> affectedPlatforms, Runnable test) {
        String platform = Platforms.current();
        if (!affectedPlatforms.contains(platform)) {
            test.run();
            return;
        }
        try {
            test.run();
        } catch (GameTestAssertException e) {
            BucketLib.LOG.warn("Known issue still present on {} ({}): {}", platform, issue, e.getMessage());
            return;
        }
        helper.fail("Known issue seems to be fixed on " + platform + " (" + issue + "). Remove the KnownIssues marker of this test and update TESTING.md.");
    }

}
