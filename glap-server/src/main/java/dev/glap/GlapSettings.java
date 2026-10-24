package dev.glap;

/** Immutable settings snapshot shared by the Kotlin loader and Java patch bridge. */
public record GlapSettings(
    int configVersion,
    boolean endermanPicksUpBlocks,
    boolean creeperDestroysBlocks,
    boolean farmlandTrampling,
    boolean allowWaterPlacementInEnd,
    boolean dispensersPlaceAnvils,
    boolean allowInfinityAndMendingTogether,
    boolean updatesEnabled,
    boolean paperMetricsEnabled
) {
}
