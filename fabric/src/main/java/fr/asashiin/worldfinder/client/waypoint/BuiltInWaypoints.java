package fr.asashiin.worldfinder.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-thread owner of navigation, sound feedback, and world-scoped persistence. */
public final class BuiltInWaypoints {
    private static final Logger LOGGER = LoggerFactory.getLogger("SeedSight/Waypoints");
    private static final NavigationState STATE = new NavigationState();
    private static ClientPacketListener connection;
    private static Path saveFile;
    private static NavigationTarget fadingTarget;
    private static long changedAt;
    private static boolean arrived;
    private BuiltInWaypoints() {}
    public static NavigationTarget target() { return STATE.target(); }
    public static NavigationTarget displayTarget() { return target() != null ? target() : fadingTarget; }
    public static boolean arrived() { return arrived; }
    public static long changedAt() { return changedAt; }

    public static void tick(Minecraft client) {
        if (client.getConnection() != connection) {
            reset();
            connection = client.getConnection();
            if (connection != null && client.level != null) load(client);
        }
        if (connection == null || client.level == null || client.player == null) return;
        if (saveFile == null) load(client);
        if (client.isPaused()) return;
        NavigationTarget completed = STATE.tick(client.level.dimension().identifier().toString(),
                client.player.getX(), client.player.getZ(), client.player.isAlive() && !client.player.isSpectator());
        if (completed != null) {
            fadingTarget = completed;
            arrived = true;
            changedAt = System.nanoTime();
            persist(client);
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.15F, 0.32F));
        }
    }
    public static boolean set(NavigationTarget target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;
        tick(client);
        if (saveFile == null) return false;
        STATE.set(target);
        fadingTarget = null;
        arrived = false;
        changedAt = System.nanoTime();
        persist(client);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F, 0.3F));
        return true;
    }
    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        if (target() == null) return;
        fadingTarget = target();
        STATE.set(null);
        arrived = false;
        changedAt = System.nanoTime();
        persist(client);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.2F));
    }
    public static void reset() {
        STATE.set(null);
        connection = null;
        saveFile = null;
        fadingTarget = null;
        arrived = false;
        changedAt = 0;
        WaypointHud.reset();
    }
    private static void load(Minecraft client) {
        String identity;
        if (client.getSingleplayerServer() != null) {
            identity = "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        } else if (client.getCurrentServer() != null) {
            identity = "server:" + client.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
        } else return;
        saveFile = client.gameDirectory.toPath().resolve("config/seedsight/waypoints")
                .resolve(WaypointStore.identityHash(identity) + ".properties");
        try {
            STATE.set(WaypointStore.load(saveFile));
            changedAt = System.nanoTime();
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read SeedSight waypoint {}", saveFile, e);
        }
    }
    private static void persist(Minecraft client) {
        if (saveFile == null) return;
        try { WaypointStore.save(saveFile, target()); }
        catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not save SeedSight waypoint {}", saveFile, e);
            if (client.player != null) client.player.sendSystemMessage(Component.literal("SeedSight: waypoint could not be saved to disk."));
        }
    }
}
