package com.bgsoftware.ssbslimeworldmanager.tasks;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.utils.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldUnloadTask extends BukkitRunnable {

    private static final SuperiorSkyblock plugin = SuperiorSkyblockAPI.getSuperiorSkyblock();

    private static final Map<String, WorldUnloadTask> worldUnloadTasks = new ConcurrentHashMap<>();

    private final String worldName;

    // gets the last time the island was accessed on the server (in seconds)
    // if it hasn't been accessed within the unload delay, the world will be unloaded
    // even if an island member is online, the world can still be unloaded; it will just load on island access
    private long lastAccessedTimestamp = System.currentTimeMillis() / 1000L;

    private WorldUnloadTask(String worldName) {
        this.worldName = worldName;

        int unloadDelay = SlimeWorldModule.getConfigSettings().getUnloadDelay();
        runTaskTimer(plugin, 20L, unloadDelay * 20L);
    }

    public void updateLastTimeAccessed() {
        lastAccessedTimestamp = System.currentTimeMillis() / 1000L;
    }

    @Override
    public void run() {
        final World world = Bukkit.getWorld(worldName);

        if (world == null) {
            stopTask(worldName);
            return;
        }

        long currentTime = System.currentTimeMillis() / 1000;

        if ((currentTime - lastAccessedTimestamp > SlimeWorldModule.getConfigSettings().getUnloadDelay()) && world.getPlayers().isEmpty())
            Bukkit.getScheduler().runTask(plugin, () -> SlimeUtils.saveAndUnloadWorld(world));
    }

    public static WorldUnloadTask getTask(String worldName) {
        return worldUnloadTasks.computeIfAbsent(worldName, w -> new WorldUnloadTask(worldName));
    }

    public static void stopTask(String worldName) {
        WorldUnloadTask worldUnloadTask = worldUnloadTasks.remove(worldName);
        if (worldUnloadTask != null)
            worldUnloadTask.cancel();
    }

}
