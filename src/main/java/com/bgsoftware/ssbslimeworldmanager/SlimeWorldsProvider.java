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
        ISlimeWorld slimeWorld = this.module.getSlimeAdapter().loadAndGetWorld(islandUUID, World.Environment.NORMAL);
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
        return true;
    }

    @Override
    public boolean isNormalUnlocked() {
        return true;
    }

    @Override
    public boolean isNetherEnabled() {
        return true;
    }

    @Override
    public boolean isNetherUnlocked() {
        return false;
    }

    @Override
    public boolean isEndEnabled() {
        return true;
    }

    @Override
    public boolean isEndUnlocked() {
        return false;
    }

}