package com.bgsoftware.ssbslimeworldmanager.hook;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.tasks.WorldUnloadTask;
import com.bgsoftware.ssbslimeworldmanager.utils.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.hooks.LazyWorldsProvider;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.WorldInfo;
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
    private final SlimeWorldModule module;

    public SlimeWorldsProvider(SlimeWorldModule module) {
        this.module = module;
    }

    @Override
    public void prepareWorlds() {

    }

    @Override
    public World getIslandsWorld(Island island, World.Environment environment) {
        return isEnvironmentEnabled(environment) ? getSlimeWorldAsBukkit(island.getUniqueId(), environment) : null;
    }

    @Override
    public boolean isIslandsWorld(World world) {
        return SlimeUtils.isIslandsWorld(world.getName());
    }

    @Override
    public Location getNextLocation(Location previousLocation, int islandsHeight, int maxIslandSize, UUID islandOwner, UUID islandUUID) {
        // The world should be loaded by now.
        World islandWorld = getSlimeWorldAsBukkit(islandUUID, module.getPlugin().getSettings().getWorlds().getDefaultWorld());
        return new Location(islandWorld, 0, islandsHeight, 0);
    }

    @Override
    public void finishIslandCreation(Location islandsLocation, UUID islandOwner, UUID islandUUID) {

    }

    @Override
    public void prepareTeleport(Island island, Location location, Runnable finishCallback) {
        prepareWorld(island, location.getWorld().getEnvironment(), finishCallback);
    }

    @Override
    public boolean isNormalEnabled() {
        return module.getPlugin().getSettings().getWorlds().getNormal().isEnabled();
    }

    @Override
    public boolean isNormalUnlocked() {
        return isNormalEnabled() && module.getPlugin().getSettings().getWorlds().getNormal().isUnlocked();
    }

    @Override
    public boolean isNetherEnabled() {
        return module.getPlugin().getSettings().getWorlds().getNether().isEnabled();
    }

    @Override
    public boolean isNetherUnlocked() {
        return isNetherEnabled() && module.getPlugin().getSettings().getWorlds().getNether().isUnlocked();
    }

    @Override
    public boolean isEndEnabled() {
        return module.getPlugin().getSettings().getWorlds().getEnd().isEnabled();
    }

    @Override
    public boolean isEndUnlocked() {
        return isEndEnabled() && module.getPlugin().getSettings().getWorlds().getEnd().isUnlocked();
    }

    @Nullable
    @Override
    public WorldInfo getIslandsWorldInfo(Island island, World.Environment environment) {
        return WorldInfo.of(SlimeUtils.getWorldName(island.getUniqueId(), environment), environment);
    }

    @Nullable
    @Override
    public WorldInfo getIslandsWorldInfo(Island island, String worldName) {
        World.Environment environment = SlimeUtils.getEnvironment(worldName);
        if (environment == null) return null;
        return WorldInfo.of(worldName, environment);
    }

    public void prepareWorld(Island island, World.Environment environment, Runnable finishCallback) {
        if (island.isSpawn()) {
            finishCallback.run();
            return;
        }

        getSlimeWorldAsBukkitAsync(island.getUniqueId(), environment).whenComplete((world, error) -> {
            if (error != null) {
                error.printStackTrace();
            } else {
                finishCallback.run();
            }
        });
    }

    public World getSlimeWorldAsBukkit(UUID islandUUID, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(islandUUID, environment);

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
                return getSlimeWorldAsBukkitLocked(worldName, environment, pendingRequest);
            }
        }

        return getSlimeWorldAsBukkitLocked(worldName, environment, pendingRequest);
    }

    private World getSlimeWorldAsBukkitLocked(String worldName, World.Environment environment, @Nullable PendingWorldLoadRequest pendingRequest) {
        // We load the world synchronized as we need it right now.
        ISlimeWorld slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, environment);
        World bukkitWorld = generateWorld(slimeWorld);

        WorldUnloadTask.getTask(slimeWorld.getName()).updateTimeUntilNextUnload();

        if (pendingRequest != null) {
            pendingRequest.complete(bukkitWorld);
        }

        return bukkitWorld;
    }

    public CompletableFuture<World> getSlimeWorldAsBukkitAsync(UUID islandUUID, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(islandUUID, environment);

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
                slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, environment);
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
                result.complete(bukkitWorld);
                WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
            });
        });

        return result;
    }

    private World generateWorld(ISlimeWorld slimeWorld) {
        this.module.getSlimeAdapter().generateWorld(slimeWorld);
        World bukkitWorld = Bukkit.getWorld(slimeWorld.getName());
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(bukkitWorld));
        return bukkitWorld;
    }

    private boolean isEnvironmentEnabled(World.Environment environment) {
        switch (environment) {
            case NORMAL:
                return isNormalEnabled();
            case NETHER:
                return isNetherEnabled();
            case THE_END:
                return isEndEnabled();
            default:
                return false;
        }
    }

    private static class PendingWorldLoadRequest extends CompletableFuture<World> {

        private final Object mutex = new Object();
        private boolean isStopped = false;

    }

}
