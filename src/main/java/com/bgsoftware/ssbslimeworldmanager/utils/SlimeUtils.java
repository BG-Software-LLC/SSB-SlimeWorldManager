package com.bgsoftware.ssbslimeworldmanager.utils;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SlimeUtils {

    private static final Map<String, ISlimeWorld> islandWorlds = new ConcurrentHashMap<>();

    private SlimeUtils() {
    }

    public static ISlimeWorld getSlimeWorld(String worldName) {
        return islandWorlds.get(worldName);
    }

    public static void setSlimeWorld(String worldName, ISlimeWorld slimeWorld) {
        islandWorlds.put(worldName, slimeWorld);
    }

    public static boolean isIslandsWorld(String worldName) {
        return islandWorlds.get(worldName) != null;
    }

    public static String getWorldName(UUID islandUUID, World.Environment environment) {
        return "island_" + islandUUID + "_" + environment.name().toLowerCase();
    }

    @Nullable
    public static World.Environment getEnvironment(String worldName) {
        String[] nameSections = worldName.split("_");

        if(nameSections.length != 3)
            return null;

        return World.Environment.valueOf(nameSections[2].toUpperCase(Locale.ENGLISH));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isIslandWorldName(String worldName) {
        String[] nameSections = worldName.split("_");

        if (nameSections.length != 3)
            return false;

        try {
            UUID.fromString(nameSections[1]);
            World.Environment.valueOf(nameSections[2].toUpperCase(Locale.ENGLISH));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static void saveAndUnloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        world.save();
        Bukkit.unloadWorld(world, false);

        islandWorlds.remove(worldName);
    }

    public static void saveAndUnloadWorld(World world) {
        if(world == null) return;

        world.save();
        Bukkit.unloadWorld(world, false);

        islandWorlds.remove(world.getName());
    }

}
