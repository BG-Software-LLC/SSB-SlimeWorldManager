package com.bgsoftware.ssbslimeworldmanager.tasks;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldUnloadTask extends BukkitRunnable {

    private static final long MINUTE_IN_TICKS = 1200L;

    private static final WorldUnloadTask EMPTY_TASK = new WorldUnloadTask(null) {
        @Override
        public void run() {
            // If this will ever get called, it should do nothing and cancel itself.
            cancel();
        }

        @Override
        public void updateTimeUntilNextUnload() {
            // Do nothing.
        }
    };

    private static final SlimeWorldModule module = SlimeWorldModule.getModule();

    private static final Map<String, WorldUnloadTask> worldUnloadTasks = new ConcurrentHashMap<>();

    private final String worldName;

    private long timeUntilNextUnload;

    private WorldUnloadTask(String worldName) {
        Preconditions.checkState(worldName == null || module.getSettings().unloadDelay > 0,
                "Cannot create unload task with negative delay.");

        this.worldName = worldName;

        if (worldName != null && module.getSettings().unloadDelay > 0) {
            runTaskTimer(module.getPlugin(), MINUTE_IN_TICKS, module.getSettings().unloadDelay * MINUTE_IN_TICKS);
            updateTimeUntilNextUnload();
        }
    }

    public void updateTimeUntilNextUnload() {
        this.timeUntilNextUnload = module.getSettings().unloadDelay;
    }

    @Override
    public void run() {
        if (timeUntilNextUnload-- > 0)
            return;

        final World world = Bukkit.getWorld(worldName);

        if (world == null) {
            cancel();
            return;
        }

        if (!world.getPlayers().isEmpty() || module.getProviders().shouldKeepWorldLoaded(world)) {
            updateTimeUntilNextUnload();
            return;
        }

        SlimeUtils.saveAndUnloadWorld(world);
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        worldUnloadTasks.remove(this.worldName);
        super.cancel();
    }

    public static WorldUnloadTask getTask(String worldName) {
        return module.getSettings().unloadDelay <= 0 ? EMPTY_TASK :
                worldUnloadTasks.computeIfAbsent(worldName, w -> new WorldUnloadTask(worldName));
    }

}
