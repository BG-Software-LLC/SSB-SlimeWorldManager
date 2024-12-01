package com.bgsoftware.ssbslimeworldmanager.api;

import com.bgsoftware.superiorskyblock.api.world.Dimension;
import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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

    public static String getWorldName(UUID islandUUID, Dimension dimension) {
        return "island_" + islandUUID + "_" + dimension.getName().toLowerCase();
    }

    @Nullable
    public static Dimension getDimension(String worldName) {
        String[] nameSections = worldName.split("_");

        if (nameSections.length < 3)
            return null;

        StringBuilder environmentName = new StringBuilder();
        for (int i = 2; i < nameSections.length; ++i) {
            environmentName.append("_").append(nameSections[i]);
        }

        return Dimension.getByName(environmentName.substring(1).toUpperCase(Locale.ENGLISH));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isIslandWorldName(String worldName) {
        String[] nameSections = worldName.split("_");

        if (nameSections.length < 3)
            return false;

        StringBuilder environmentName = new StringBuilder();
        for (int i = 2; i < nameSections.length; ++i) {
            environmentName.append("_").append(nameSections[i]);
        }

        try {
            UUID.fromString(nameSections[1]);
            World.Environment.valueOf(environmentName.substring(1).toUpperCase(Locale.ENGLISH));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static void saveAndUnloadWorld(String worldName) {
        saveAndUnloadWorld(Bukkit.getWorld(worldName));
    }

    public static void saveAndUnloadWorld(@Nullable World world) {
        if (world == null)
            return;

        world.save();
        Bukkit.unloadWorld(world, false);
    }

    public static void notifyUnloadWorld(World world) {
        islandWorlds.remove(world.getName());
    }

}
