package dev.glap.internal;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class BundledKodeInstaller {
    private static final String RESOURCE_PATH = "/META-INF/glap/kode.jar";
    private static final String PLUGIN_FILE_NAME = "Kode.jar";
    private static final String MANAGED_HASH_FILE_NAME = ".glap-kode.sha256";

    private BundledKodeInstaller() {}

    public static void installIfMissing(final Path pluginsDirectory, final Logger logger) throws IOException {
        try (InputStream bundledKode = BundledKodeInstaller.class.getResourceAsStream(RESOURCE_PATH)) {
            if (bundledKode == null) {
                logger.debug("No bundled Kode plugin was found; skipping automatic installation.");
                return;
            }

            Files.createDirectories(pluginsDirectory);
            final Path pluginFile = pluginsDirectory.resolve(PLUGIN_FILE_NAME);
            final Path managedHashFile = pluginsDirectory.resolve(MANAGED_HASH_FILE_NAME);

            if (Files.exists(pluginFile, LinkOption.NOFOLLOW_LINKS) && !Files.exists(managedHashFile, LinkOption.NOFOLLOW_LINKS)) {
                logger.info("A Kode plugin already exists; leaving the existing plugin untouched.");
                return;
            }

            final Path temporaryPlugin = Files.createTempFile(pluginsDirectory, ".glap-kode-", ".jar.tmp");
            try {
                Files.copy(bundledKode, temporaryPlugin, StandardCopyOption.REPLACE_EXISTING);
                final String bundledHash = sha256(temporaryPlugin);

                if (Files.exists(pluginFile, LinkOption.NOFOLLOW_LINKS)) {
                    final String managedHash = Files.readString(managedHashFile).trim();
                    final String installedHash = sha256(pluginFile);
                    if (!installedHash.equals(managedHash)) {
                        if (installedHash.equals(bundledHash)) {
                            Files.writeString(managedHashFile, bundledHash + System.lineSeparator());
                            return;
                        }
                        logger.warn("Kode.jar was changed after GLAP installed it; preserving the current file.");
                        return;
                    }
                    if (installedHash.equals(bundledHash)) {
                        return;
                    }

                    move(temporaryPlugin, pluginFile, true);
                    logger.info("Updated the GLAP-managed Kode plugin.");
                } else {
                    try {
                        move(temporaryPlugin, pluginFile, false);
                        logger.info("Installed the bundled Kode plugin.");
                    } catch (final FileAlreadyExistsException ignored) {
                        logger.info("A Kode plugin appeared during startup; leaving the existing plugin untouched.");
                        return;
                    }
                }

                Files.writeString(managedHashFile, bundledHash + System.lineSeparator());
            } finally {
                Files.deleteIfExists(temporaryPlugin);
            }
        }
    }

    private static String sha256(final Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void move(final Path source, final Path target, final boolean replaceExisting) throws IOException {
        if (!replaceExisting) {
            Files.move(source, target);
            return;
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
