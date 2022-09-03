package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.hooks.WorldsProvider;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class SlimeWorldsProvider implements WorldsProvider {

    private final SlimeWorldModule module;

    public SlimeWorldsProvider(SlimeWorldModule module) {
        this.module = module;
    }

    @Override
    public void prepareWorlds() {

    }

    @Override
    public World getIslandsWorld(Island island, World.Environment environment) {
        if (!isEnvironmentEnabled(environment))
            return null;

        ISlimeWorld slimeWorld = this.module.getSlimeAdapter().loadAndGetWorld(island, environment);
        WorldUnloadTask.getTask(slimeWorld.getName()).updateLastTime();

        return Bukkit.getWorld(slimeWorld.getName());
    }

    @Override
    public boolean isIslandsWorld(World world) {
        return SlimeUtils.isIslandsWorld(world.getName());
    }

    @Override
    public Location getNextLocation(Location previousLocation, int islandsHeight, int maxIslandSize, UUID islandOwner, UUID islandUUID) {
        ISlimeWorld slimeWorld = this.module.getSlimeAdapter().loadAndGetWorld(islandUUID,
                module.getPlugin().getSettings().getWorlds().getDefaultWorld());
        WorldUnloadTask.getTask(slimeWorld.getName());
        return new Location(Bukkit.getWorld(slimeWorld.getName()), 0, islandsHeight, 0);
    }

    @Override
    public void finishIslandCreation(Location islandsLocation, UUID islandOwner, UUID islandUUID) {

    }

    @Override
    public void prepareTeleport(Island island, Location location, Runnable finishCallback) {
        if (!island.isSpawn()) {
            ISlimeWorld slimeWorld = this.module.getSlimeAdapter().loadAndGetWorld(island, location.getWorld().getEnvironment());
            WorldUnloadTask.getTask(slimeWorld.getName()).updateLastTime();
        }
        finishCallback.run();
    }

    @Override
    public boolean isNormalEnabled() {
        return module.getPlugin().getSettings().getWorlds().getNormal().isEnabled();
    }

    @Override
    public boolean isNormalUnlocked() {
        return module.getPlugin().getSettings().getWorlds().getNormal().isUnlocked();
    }

    @Override
    public boolean isNetherEnabled() {
        return module.getPlugin().getSettings().getWorlds().getNether().isEnabled();
    }

    @Override
    public boolean isNetherUnlocked() {
        return module.getPlugin().getSettings().getWorlds().getNether().isUnlocked();
    }

    @Override
    public boolean isEndEnabled() {
        return module.getPlugin().getSettings().getWorlds().getEnd().isEnabled();
    }

    @Override
    public boolean isEndUnlocked() {
        return module.getPlugin().getSettings().getWorlds().getEnd().isUnlocked();
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