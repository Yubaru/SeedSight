package fr.asashiin.worldfinder.client.waypoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;

/** One small atomic file per world/server. Empty files represent a cleared destination. */
public final class WaypointStore {
    private WaypointStore() {}

    public static String identityHash(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static NavigationTarget load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return null;
        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
        if (!values.containsKey("name")) return null;
        try {
            return new NavigationTarget(values.getProperty("name"), values.getProperty("dimension"),
                    Integer.parseInt(values.getProperty("x")), Integer.parseInt(values.getProperty("y")),
                    Integer.parseInt(values.getProperty("z")));
        } catch (IllegalArgumentException e) { throw new IOException("Invalid saved waypoint", e); }
    }

    public static void save(Path path, NavigationTarget target) throws IOException {
        Files.createDirectories(path.getParent());
        Properties values = new Properties();
        if (target != null) {
            values.setProperty("name", target.name());
            values.setProperty("dimension", target.dimension());
            values.setProperty("x", Integer.toString(target.x()));
            values.setProperty("y", Integer.toString(target.y()));
            values.setProperty("z", Integer.toString(target.z()));
        }
        Path temporary = Files.createTempFile(path.getParent(), "waypoint-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { values.store(writer, "SeedSight waypoint"); }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
