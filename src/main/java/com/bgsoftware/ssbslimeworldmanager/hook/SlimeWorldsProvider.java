package com.bgsoftware.ssbslimeworldmanager.hook;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.tasks.WorldUnloadTask;
import com.bgsoftware.ssbslimeworldmanager.utils.Dimensions;
import com.bgsoftware.superiorskyblock.api.config.SettingsManager;
import com.bgsoftware.superiorskyblock.api.hooks.LazyWorldsProvider;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.bgsoftware.superiorskyblock.api.world.WorldInfo;
import com.bgsoftware.superiorskyblock.api.wrappers.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldsProvider implements LazyWorldsProvider {

    private final Map<String, PendingWorldLoadRequest> pendingWorldRequests = new HashMap<>();
    private final Map<UUID, Dimension> islandWorldsToDimensions = new HashMap<>();
    private final SlimeWorldModule module;

    public SlimeWorldsProvider(SlimeWorldModule module) {
        this.module = module;
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            Location spawnLocation = module.getPlugin().getGrid().getSpawnIsland().getCenter(Dimensions.NORMAL);
            World spawnWorld = spawnLocation.getWorld();
            if (spawnWorld != null) {
                islandWorldsToDimensions.computeIfAbsent(spawnWorld.getUID(),
                        u -> Dimension.getByName(spawnWorld.getEnvironment().name()));
            }
        }, 40L);
    }

    @Override
    public void prepareWorlds() {

    }

    @Override
    public World getIslandsWorld(Island island, Dimension dimension) {
        return isDimensionEnabled(dimension) ? getSlimeWorldAsBukkit(island.getUniqueId(), dimension) : null;
    }

    @Override
    public World getIslandsWorld(Island island, World.Environment environment) {
        return getIslandsWorld(island, Dimension.getByName(environment.name()));
    }

    @Override
    public boolean isIslandsWorld(World world) {
        return SlimeUtils.isIslandsWorld(world.getName());
    }

    @Override
    public Location getNextLocation(Location previousLocation, int islandsHeight, int maxIslandSize, UUID islandOwner, UUID islandUUID) {
        return computeNextLocationInternal(islandUUID, islandsHeight);
    }

    public Location getNextLocation(BlockPosition previousPosition, int islandsHeight, int maxIslandSize, UUID islandOwner, UUID islandUUID) {
        return computeNextLocationInternal(islandUUID, islandsHeight);
    }

    private Location computeNextLocationInternal(UUID islandUUID, int islandsHeight) {
        // The world should be loaded by now.
        World islandWorld = getSlimeWorldAsBukkit(islandUUID,
                module.getPlugin().getSettings().getWorlds().getDefaultWorldDimension());
        return new Location(islandWorld, 0, islandsHeight, 0);
    }

    @Override
    public void finishIslandCreation(Location islandsLocation, UUID islandOwner, UUID islandUUID) {

    }

    @Override
    public void prepareTeleport(Island island, Location location, Runnable finishCallback) {
        prepareWorld(island, getIslandsWorldDimension(location.getWorld()), finishCallback);
    }

    @Override
    public boolean isNormalEnabled() {
        return isDimensionEnabled(Dimensions.NORMAL);
    }

    @Override
    public boolean isNormalUnlocked() {
        return isDimensionUnlocked(Dimensions.NORMAL);
    }

    @Override
    public boolean isNetherEnabled() {
        return isDimensionEnabled(Dimensions.NETHER);
    }

    @Override
    public boolean isNetherUnlocked() {
        return isDimensionUnlocked(Dimensions.NETHER);
    }

    @Override
    public boolean isEndEnabled() {
        return isDimensionEnabled(Dimensions.THE_END);
    }

    @Override
    public boolean isEndUnlocked() {
        return isDimensionUnlocked(Dimensions.THE_END);
    }

    @Override
    public boolean isDimensionEnabled(Dimension dimension) {
        SettingsManager.Worlds.DimensionConfig dimensionConfig =
                module.getPlugin().getSettings().getWorlds().getDimensionConfig(dimension);
        // If the config is null, it probably means another plugin registered it.
        // Therefore, we register it as enabled.
        return dimensionConfig == null || dimensionConfig.isEnabled();
    }

    @Override
    public boolean isDimensionUnlocked(Dimension dimension) {
        SettingsManager.Worlds.DimensionConfig dimensionConfig =
                module.getPlugin().getSettings().getWorlds().getDimensionConfig(dimension);
        return dimensionConfig != null && dimensionConfig.isUnlocked();
    }

    @Nullable
    @Override
    public WorldInfo getIslandsWorldInfo(Island island, Dimension dimension) {
        return WorldInfo.of(SlimeUtils.getWorldName(island.getUniqueId(), dimension), dimension);
    }

    @Override
    public Dimension getIslandsWorldDimension(World world) {
        Dimension dimension = islandWorldsToDimensions.get(world.getUID());
        if (dimension != null)
            return dimension;
        return Dimension.getByName(world.getEnvironment().name());
    }

    @Nullable
    @Override
    public WorldInfo getIslandsWorldInfo(Island island, String worldName) {
        Dimension dimension = SlimeUtils.getDimension(worldName);
        if (dimension == null)
            return null;
        return WorldInfo.of(worldName, dimension);
    }

    @Override
    public void prepareWorld(Island island, Dimension dimension, Runnable finishCallback) {
        if (island.isSpawn()) {
            finishCallback.run();
            return;
        }

        getSlimeWorldAsBukkitAsync(island.getUniqueId(), dimension).whenComplete((world, error) -> {
            if (error != null) {
                error.printStackTrace();
            } else {
                finishCallback.run();
            }
        });
    }

    public World getSlimeWorldAsBukkit(UUID islandUUID, Dimension dimension) {
        String worldName = SlimeUtils.getWorldName(islandUUID, dimension);

        {
            World bukkitWorld = Bukkit.getWorld(worldName);

            if (bukkitWorld != null) {
                WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
                return bukkitWorld;
            }
        }

        PendingWorldLoadRequest pendingRequest = pendingWorldRequests.remove(worldName);
        if (pendingRequest != null) {
            synchronized (pendingRequest.mutex) {
                pendingRequest.isStopped = true;
                return getSlimeWorldAsBukkitLocked(worldName, dimension, pendingRequest);
            }
        }

        return getSlimeWorldAsBukkitLocked(worldName, dimension, pendingRequest);
    }

    private World getSlimeWorldAsBukkitLocked(String worldName, Dimension dimension, @Nullable PendingWorldLoadRequest pendingRequest) {
        // We load the world synchronized as we need it right now.
        ISlimeWorld slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, dimension);
        World bukkitWorld = generateWorld(slimeWorld);

        WorldUnloadTask.getTask(slimeWorld.getName()).updateTimeUntilNextUnload();

        islandWorldsToDimensions.put(bukkitWorld.getUID(), dimension);

        if (pendingRequest != null) {
            pendingRequest.complete(bukkitWorld);
        }

        return bukkitWorld;
    }

    public CompletableFuture<World> getSlimeWorldAsBukkitAsync(UUID islandUUID, Dimension dimension) {
        String worldName = SlimeUtils.getWorldName(islandUUID, dimension);

        {
            World bukkitWorld = Bukkit.getWorld(worldName);

            if (bukkitWorld != null) {
                WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
                return CompletableFuture.completedFuture(bukkitWorld);
            }
        }

        PendingWorldLoadRequest pendingRequest = pendingWorldRequests.get(worldName);
        if (pendingRequest != null)
            return pendingRequest;

        PendingWorldLoadRequest result = new PendingWorldLoadRequest();
        pendingWorldRequests.put(worldName, result);

        Bukkit.getScheduler().runTaskAsynchronously(module.getPlugin(), () -> {
            ISlimeWorld slimeWorld;
            synchronized (result.mutex) {
                if (result.isStopped)
                    return;

                // Loading the world asynchronous.
                slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, dimension);
            }

            Bukkit.getScheduler().runTask(module.getPlugin(), () -> {
                World bukkitWorld;
                synchronized (result.mutex) {
                    if (result.isStopped)
                        return;

                    // Generating the world synchronized
                    bukkitWorld = generateWorld(slimeWorld);
                }
                pendingWorldRequests.remove(worldName);

                islandWorldsToDimensions.put(bukkitWorld.getUID(), dimension);

                result.complete(bukkitWorld);
                WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
            });
        });

        return result;
    }

    private World generateWorld(ISlimeWorld slimeWorld) {
        World bukkitWorld = Bukkit.getWorld(slimeWorld.getName());
        // Do not generate the load if it is already loaded somehow
        if (bukkitWorld != null)
            return bukkitWorld;

        this.module.getSlimeAdapter().generateWorld(slimeWorld);
        bukkitWorld = Bukkit.getWorld(slimeWorld.getName());
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(bukkitWorld));
        return bukkitWorld;
    }

    private static class PendingWorldLoadRequest extends CompletableFuture<World> {

        private final Object mutex = new Object();
        private boolean isStopped = false;

    }

}
