package dev.glap;

/**
 * Looks up whether a newer GLAP release exists. The implementation is written in Kotlin
 * ({@code GitHubUpdateChecker}) and loaded through {@link java.util.ServiceLoader}, so Java code never
 * names a Kotlin class directly.
 */
public interface UpdateChecker {
    Status check(String currentVersion);

    sealed interface Status {
        record UpToDate() implements Status {
        }

        record Outdated(String latest) implements Status {
        }

        record Unknown() implements Status {
        }
    }
}
