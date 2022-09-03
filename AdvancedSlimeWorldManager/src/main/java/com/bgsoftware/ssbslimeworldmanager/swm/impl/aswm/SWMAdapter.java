package com.bgsoftware.ssbslimeworldmanager.swm.impl.aswm;

import com.bgsoftware.ssbslimeworldmanager.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Locale;

public final class SWMAdapter implements ISlimeAdapter {

    private final SuperiorSkyblock plugin;
    private final SlimePlugin slimePlugin;
    private final WorldData defaultWorldData;

    public SWMAdapter(SuperiorSkyblock plugin) {
        this.plugin = plugin;
        this.slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        this.defaultWorldData = buildDefaultWorldData();
    }

    @Override
    public void unloadAllWorlds() {
        try {
            slimePlugin.getLoader(defaultWorldData.getDataSource()).listWorlds().forEach(worldName -> {
                if (SlimeUtils.isIslandWorldName(worldName) && Bukkit.getWorld(worldName) != null)
                    SlimeUtils.unloadWorld(worldName);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ISlimeWorld loadAndGetWorld(String worldName, World.Environment environment) {
        ISlimeWorld slimeWorld = SlimeUtils.getSlimeWorld(worldName);

        if (slimeWorld == null) {
            WorldData worldData = ConfigManager.getWorldConfig().getWorlds().get(worldName);

            try {
                // No world was found, creating a new world.
                if (worldData == null) {
                    SlimePropertyMap slimePropertyMap = defaultWorldData.toPropertyMap();
                    slimePropertyMap.setString(SlimeProperties.ENVIRONMENT, environment.name().toUpperCase());
                    slimeWorld = new SWMSlimeWorld(slimePlugin.createEmptyWorld(slimePlugin.getLoader(defaultWorldData.getDataSource()),
                            worldName, defaultWorldData.isReadOnly(), slimePropertyMap));

                    new Exception().printStackTrace();

                    // Saving the world
                    WorldsConfig config = ConfigManager.getWorldConfig();
                    config.getWorlds().put(worldName, defaultWorldData);
                    config.save();
                } else {
                    slimeWorld = new SWMSlimeWorld(slimePlugin.loadWorld(slimePlugin.getLoader(worldData.getDataSource()),
                            worldName, worldData.isReadOnly(), worldData.toPropertyMap()));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        if (Bukkit.getWorld(worldName) == null)
            slimePlugin.generateWorld(((SWMSlimeWorld) slimeWorld).getHandle());

        return slimeWorld;
    }

    @Override
    public void deleteWorld(SuperiorSkyblock plugin, Island island, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(island, environment);

        SlimeUtils.unloadWorld(worldName);

        WorldData worldData = ConfigManager.getWorldConfig().getWorlds().get(worldName);

        if (worldData != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    SlimeLoader slimeLoader = slimePlugin.getLoader(worldData.getDataSource());
                    slimeLoader.deleteWorld(worldName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, 20L);
        }
    }

    private WorldData buildDefaultWorldData() {
        WorldData worldData = new WorldData();

        worldData.setDataSource("file");
        worldData.setDifficulty(plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
        worldData.setLoadOnStartup(false);

        return worldData;
    }

}
