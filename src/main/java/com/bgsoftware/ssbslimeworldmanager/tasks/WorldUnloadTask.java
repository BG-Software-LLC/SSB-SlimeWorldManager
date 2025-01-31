package com.bgsoftware.ssbslimeworldmanager.tasks;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldUnloadTask {

    private static final long MINUTE_IN_TICKS = 1200L;

    private static final WorldUnloadTask EMPTY_TASK = new WorldUnloadTask(null) {
        @Override
        public void updateTimeUntilNextUnload() {
            // Do nothing.
        }
    };

    private static final SlimeWorldModule module = SlimeWorldModule.getModule();

    private static final Map<String, WorldUnloadTask> worldUnloadTasks = new ConcurrentHashMap<>();

    private final String worldName;

    @Nullable
    private BukkitTask currentUnloadTask;

    private WorldUnloadTask(String worldName) {
        Preconditions.checkState(worldName == null || module.getSettings().unloadDelay > 0,
                "Cannot create unload task with negative delay.");

        this.worldName = worldName;

        if (worldName != null && module.getSettings().unloadDelay > 0) {
            updateTimeUntilNextUnload();
        }
    }

    public void updateTimeUntilNextUnload() {
        if (this.currentUnloadTask != null)
            this.currentUnloadTask.cancel();

        this.currentUnloadTask = Bukkit.getScheduler().runTaskLater(
                module.getPlugin(), this::unloadTask, module.getSettings().unloadDelay * MINUTE_IN_TICKS);
    }

    private void unloadTask() {
        this.currentUnloadTask = null;

        final World world = Bukkit.getWorld(worldName);

        if (world != null) {
            if (!world.getPlayers().isEmpty() || module.getProviders().shouldKeepWorldLoaded(world)) {
                updateTimeUntilNextUnload();
                return;
            }

            SlimeUtils.saveAndUnloadWorld(world);
        }

        finishUnloadTask();
    }

    private void finishUnloadTask() {
        worldUnloadTasks.remove(this.worldName);
    }

    public static WorldUnloadTask getTask(String worldName) {
        return module.getSettings().unloadDelay <= 0 ? EMPTY_TASK :
                worldUnloadTasks.computeIfAbsent(worldName, w -> new WorldUnloadTask(worldName));
    }

}
