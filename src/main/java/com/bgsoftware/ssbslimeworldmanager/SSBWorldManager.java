package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.superiorskyblock.api.hooks.WorldsProvider;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class SSBWorldManager {

    public static WorldsProvider createManager(){
        return new SlimeWorldsProvider();
    }

    private static class SlimeWorldsProvider implements WorldsProvider {

        @Override
        public void prepareWorlds() {

        }

        @Override
        public World getIslandsWorld(Island island, World.Environment environment) {
            SlimeWorld slimeWorld = SlimeUtils.loadAndGetWorld(island, environment);
            WorldUnloadTask.getTask(slimeWorld.getName()).updateLastTime();
            return Bukkit.getWorld(slimeWorld.getName());
        }

        @Override
        public boolean isIslandsWorld(World world) {
            return SlimeUtils.isIslandsWorld(world.getName());
        }

        @Override
        public Location getNextLocation(Location previousLocation, int islandsHeight, int maxIslandSize, UUID islandOwner, UUID islandUUID) {
            SlimeWorld slimeWorld = SlimeUtils.loadAndGetWorld(islandUUID, World.Environment.NORMAL);
            WorldUnloadTask.getTask(slimeWorld.getName());
            return new Location(Bukkit.getWorld(slimeWorld.getName()), 0, islandsHeight, 0);
        }

        @Override
        public void finishIslandCreation(Location islandsLocation, UUID islandOwner, UUID islandUUID) {

        }

        @Override
        public void prepareTeleport(Island island, Location location, Runnable finishCallback) {
            if(!island.isSpawn()) {
                SlimeWorld slimeWorld = SlimeUtils.loadAndGetWorld(island, location.getWorld().getEnvironment());
                WorldUnloadTask.getTask(slimeWorld.getName()).updateLastTime();
            }
            finishCallback.run();
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

}
