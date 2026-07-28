package com.bgsoftware.ssbslimeworldmanager.save;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Saves loaded island worlds to the data-source while they are still loaded.
 * <p>
 * Without this service, an island world is only written to the data-source when it is unloaded
 * ({@link com.bgsoftware.ssbslimeworldmanager.tasks.WorldUnloadTask}) or when the server shuts
 * down gracefully. A hard crash therefore reverts every loaded island back to its last save.
 * <p>
 * Saves are requested, never performed directly: every request goes into a queue that is drained
 * once a second, and a world is skipped as long as it was saved less than `min-delay` ago. This
 * keeps a burst of triggers (many players leaving at once, a wave of upgrades) from turning into
 * a burst of disk/database writes.
 */
public class IslandWorldsSaveService {

    private static final long CYCLE_INTERVAL_TICKS = 20L;

    private final Map<String, Long> lastSaveTimes = new ConcurrentHashMap<>();
    private final Map<String, SaveReason> pendingSaves = new LinkedHashMap<>();

    private final SlimeWorldModule module;

    @Nullable
    private BukkitTask cycleTask;
    @Nullable
    private BukkitTask autoSaveTask;

    public IslandWorldsSaveService(SlimeWorldModule module) {
        this.module = module;
    }

    public void start() {
        if (!module.getSettings().autoSaveEnabled)
            return;

        this.cycleTask = Bukkit.getScheduler().runTaskTimer(module.getPlugin(), this::runCycle,
                CYCLE_INTERVAL_TICKS, CYCLE_INTERVAL_TICKS);

        long intervalTicks = module.getSettings().autoSaveInterval * 20L;
        if (intervalTicks > 0) {
            this.autoSaveTask = Bukkit.getScheduler().runTaskTimer(module.getPlugin(),
                    this::requestSaveForAllLoadedWorlds, intervalTicks, intervalTicks);
        }
    }

    public void stop() {
        if (this.cycleTask != null) {
            this.cycleTask.cancel();
            this.cycleTask = null;
        }
        if (this.autoSaveTask != null) {
            this.autoSaveTask.cancel();
            this.autoSaveTask = null;
        }
        synchronized (this.pendingSaves) {
            this.pendingSaves.clear();
        }
    }

    /**
     * Request a save for all the loaded worlds of an island.
     * The save is not guaranteed to happen immediately - see {@link #requestSave(World, SaveReason)}.
     */
    public void requestSave(Island island, SaveReason reason) {
        if (island == null || island.isSpawn())
            return;

        for (Dimension dimension : Dimension.values()) {
            World world = Bukkit.getWorld(SlimeUtils.getWorldName(island.getUniqueId(), dimension));
            if (world != null)
                requestSave(world, reason);
        }
    }

    /**
     * Request a save for a loaded island world.
     * <p>
     * The request is ignored when auto-saving is disabled, when the given reason is a trigger that
     * was turned off in the config, or when the world is not an island world. Otherwise the world is
     * queued, and it will be saved by the next cycle in which `min-delay` has passed since its last save.
     */
    public void requestSave(World world, SaveReason reason) {
        if (world == null || !module.getSettings().autoSaveEnabled)
            return;

        if (reason.isTrigger() && !module.getSettings().isSaveTriggerEnabled(reason))
            return;

        if (!SlimeUtils.isIslandsWorld(world.getName()))
            return;

        synchronized (this.pendingSaves) {
            // The first reason that queued the world is the one we report, as it is the one that
            // actually caused the save to be scheduled.
            if (!this.pendingSaves.containsKey(world.getName()))
                this.pendingSaves.put(world.getName(), reason);
        }
    }

    /**
     * Save a loaded island world right now, ignoring `min-delay`.
     * Should only be used for shutdown-like flows - regular callers want {@link #requestSave(World, SaveReason)}.
     */
    public void saveNow(World world, SaveReason reason) {
        synchronized (this.pendingSaves) {
            this.pendingSaves.remove(world.getName());
        }
        saveWorldInternal(world, reason);
    }

    /**
     * Drop all the state we hold for a world that is no longer loaded.
     */
    public void notifyWorldUnloaded(String worldName) {
        synchronized (this.pendingSaves) {
            this.pendingSaves.remove(worldName);
        }
        this.lastSaveTimes.remove(worldName);
    }

    public long getLastSaveTime(String worldName) {
        Long lastSaveTime = this.lastSaveTimes.get(worldName);
        return lastSaveTime == null ? 0L : lastSaveTime;
    }

    private void requestSaveForAllLoadedWorlds() {
        for (World world : Bukkit.getWorlds())
            requestSave(world, SaveReason.AUTO_SAVE);
    }

    private void runCycle() {
        int savesLeft = module.getSettings().autoSaveSavesPerCycle;
        if (savesLeft <= 0)
            return;

        long minDelay = module.getSettings().autoSaveMinDelay * 1000L;
        long currentTime = System.currentTimeMillis();

        while (savesLeft > 0) {
            String worldName = null;
            SaveReason reason = null;

            synchronized (this.pendingSaves) {
                Iterator<Map.Entry<String, SaveReason>> iterator = this.pendingSaves.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, SaveReason> entry = iterator.next();

                    if (currentTime - getLastSaveTime(entry.getKey()) < minDelay) {
                        // Saved too recently; keep it queued so it is saved once the delay has passed.
                        continue;
                    }

                    worldName = entry.getKey();
                    reason = entry.getValue();
                    iterator.remove();
                    break;
                }
            }

            if (worldName == null)
                return;

            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                // The world was unloaded in the meantime; it was already saved by the unload task.
                this.lastSaveTimes.remove(worldName);
                continue;
            }

            saveWorldInternal(world, reason);
            --savesLeft;
        }
    }

    private void saveWorldInternal(World world, SaveReason reason) {
        this.lastSaveTimes.put(world.getName(), System.currentTimeMillis());

        try {
            world.save();
        } catch (Throwable error) {
            module.getPlugin().getLogger().log(Level.SEVERE,
                    "An unexpected error occurred while saving island world " + world.getName() +
                            " (reason: " + reason.getConfigKey() + ")", error);
            return;
        }

        module.getPlugin().getLogger().fine(() -> "Saved island world " + world.getName() +
                " (reason: " + reason.getConfigKey() + ")");
    }

}
