package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldUnloadTask extends BukkitRunnable {

    private static final SuperiorSkyblock plugin = SuperiorSkyblockAPI.getSuperiorSkyblock();

    private static final Map<String, WorldUnloadTask> worldTasks = new ConcurrentHashMap<>();
    private static final long UNLOAD_DELAY = 24000;

    private final String worldName;
    private long lastTimeUpdate = System.currentTimeMillis() / 1000;

    private WorldUnloadTask(String worldName) {
        this.worldName = worldName;
        runTaskTimer(plugin, UNLOAD_DELAY, UNLOAD_DELAY);
    }

    public void updateLastTime() {
        lastTimeUpdate = System.currentTimeMillis() / 1000;
    }

    @Override
    public void run() {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            stopTask(worldName);
            return;
        }

        long currentTime = System.currentTimeMillis() / 1000;

        if (currentTime - lastTimeUpdate > UNLOAD_DELAY && world.getPlayers().isEmpty()) {
            SlimeUtils.unloadWorld(worldName, true);
        } else {
            updateLastTime();
        }
    }

    public static WorldUnloadTask getTask(String worldName) {
        return worldTasks.computeIfAbsent(worldName, w -> new WorldUnloadTask(worldName));
    }

    public static void stopTask(String worldName) {
        WorldUnloadTask worldUnloadTask = worldTasks.remove(worldName);
        if (worldUnloadTask != null)
            worldUnloadTask.cancel();
    }

}
