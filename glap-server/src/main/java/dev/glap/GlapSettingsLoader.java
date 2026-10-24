package dev.glap;

import java.io.File;

/** Kotlin-backed configuration parsing boundary for the Java server integration. */
public interface GlapSettingsLoader {
    GlapSettings load(File file) throws Exception;
}
