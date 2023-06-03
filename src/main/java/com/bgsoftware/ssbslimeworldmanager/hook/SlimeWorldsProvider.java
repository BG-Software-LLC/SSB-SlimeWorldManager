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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldsProvider implements LazyWorldsProvider {

    private final Map<String, CompletableFuture<World>> pendingWorldRequests = new HashMap<>();
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
        if (island.isSpawn()) {
            finishCallback.run();
            return;
        }

        getSlimeWorldAsBukkitAsync(island.getUniqueId(), location.getWorld().getEnvironment()).whenComplete((world, error) -> {
            if (error != null) {
                error.printStackTrace();
            } else {
                finishCallback.run();
            }
        });
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

    public World getSlimeWorldAsBukkit(UUID islandUUID, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(islandUUID, environment);
        World bukkitWorld = Bukkit.getWorld(worldName);

        if (bukkitWorld != null) {
            WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
            return bukkitWorld;
        }

        // We load the world synchronized as we need it right now.
        ISlimeWorld slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, environment);

        this.module.getSlimeAdapter().generateWorld(slimeWorld);
        WorldUnloadTask.getTask(slimeWorld.getName()).updateTimeUntilNextUnload();

        World result = Bukkit.getWorld(slimeWorld.getName());

        CompletableFuture<World> pendingRequest = pendingWorldRequests.remove(worldName);
        if (pendingRequest != null)
            pendingRequest.complete(result);

        return result;
    }

    public CompletableFuture<World> getSlimeWorldAsBukkitAsync(UUID islandUUID, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(islandUUID, environment);
        World bukkitWorld = Bukkit.getWorld(worldName);

        if (bukkitWorld != null) {
            WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
            return CompletableFuture.completedFuture(bukkitWorld);
        }

        CompletableFuture<World> pendingRequest = pendingWorldRequests.get(worldName);
        if (pendingRequest != null)
            return pendingRequest;

        CompletableFuture<World> result = new CompletableFuture<>();
        pendingWorldRequests.put(worldName, result);

        Bukkit.getScheduler().runTaskAsynchronously(module.getPlugin(), () -> {
            // Loading the world asynchronous.
            ISlimeWorld slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, environment);
            Bukkit.getScheduler().runTask(module.getPlugin(), () -> {
                // Generating the world synchronized
                this.module.getSlimeAdapter().generateWorld(slimeWorld);
                pendingWorldRequests.remove(worldName);
                result.complete(Bukkit.getWorld(worldName));
                WorldUnloadTask.getTask(worldName).updateTimeUntilNextUnload();
            });
        });

        return result;
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

}
