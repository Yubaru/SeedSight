package fr.asashiin.worldfinder.seedsight;

import fr.asashiin.worldfinder.api.waypoint.WaypointRequest;
import fr.asashiin.worldfinder.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Stores independent visited checks for every structure or POI in each world/server. */
public final class VisitedPois {
    private static final Logger LOGGER = LoggerFactory.getLogger("SeedSight/VisitedPois");
    private static final Set<VisitKey> VISITED = new HashSet<>();
    private static ClientPacketListener connection;
    private static Path saveFile;

    private VisitedPois() {}

    public static void tick(Minecraft client) {
        if (client.getConnection() == connection) return;
        reset();
        connection = client.getConnection();
        if (connection != null && client.level != null) load(client);
    }

    public static boolean isVisited(WaypointRequest request) {
        tick(Minecraft.getInstance());
        return VISITED.contains(VisitKey.from(request));
    }

    public static boolean isVisited(String dimension, int x, int z) {
        tick(Minecraft.getInstance());
        return VISITED.contains(new VisitKey(dimension, x, z));
    }

    public static boolean setVisited(WaypointRequest request, boolean visited) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;
        tick(client);
        if (saveFile == null) return false;

        VisitKey key = VisitKey.from(request);
        boolean changed = visited ? VISITED.add(key) : VISITED.remove(key);
        if (changed) save(client);

        String state = visited ? "checked off as visited" : "marked unvisited";
        client.player.sendSystemMessage(Component.literal("SeedSight: " + request.name() + " " + state + "."));
        client.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.EXPERIENCE_ORB_PICKUP, visited ? 1.35F : 0.8F, 0.25F));
        return true;
    }

    public static void reset() {
        VISITED.clear();
        connection = null;
        saveFile = null;
    }

    private static void load(Minecraft client) {
        String identity;
        if (client.getSingleplayerServer() != null) {
            identity = "world:" + client.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        } else if (client.getCurrentServer() != null) {
            identity = "server:" + client.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
        } else {
            return;
        }

        saveFile = client.gameDirectory.toPath().resolve("config/seedsight/visited")
                .resolve(WaypointStore.identityHash(identity) + ".properties");
        if (!Files.isRegularFile(saveFile)) return;

        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
            values.load(reader);
            for (String property : values.stringPropertyNames()) {
                if (!property.startsWith("poi.")) continue;
                String[] parts = values.getProperty(property, "").split(",", 3);
                if (parts.length == 3) {
                    VISITED.add(new VisitKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            VISITED.clear();
            LOGGER.warn("Could not read SeedSight visited POIs {}", saveFile, e);
        }
    }

    private static void save(Minecraft client) {
        try {
            Files.createDirectories(saveFile.getParent());
            Properties values = new Properties();
            int index = 0;
            for (VisitKey key : VISITED) {
                values.setProperty("poi." + index++, key.dimension + "," + key.x + "," + key.z);
            }

            Path temporary = Files.createTempFile(saveFile.getParent(), "visited-", ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    values.store(writer, "SeedSight visited POIs");
                }
                try {
                    Files.move(temporary, saveFile, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, saveFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not save SeedSight visited POIs {}", saveFile, e);
            client.player.sendSystemMessage(Component.literal("SeedSight: visited POIs could not be saved to disk."));
        }
    }

    private record VisitKey(String dimension, int x, int z) {
        private static VisitKey from(WaypointRequest request) {
            return new VisitKey("minecraft:" + request.dimension().serializedName(),
                    request.blockX(), request.blockZ());
        }
    }
}
