package com.bgsoftware.ssbslimeworldmanager.swm.impl.aswm;

import com.bgsoftware.ssbslimeworldmanager.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.google.common.base.Preconditions;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.List;
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
    public List<String> getLoadedWorlds() throws IOException {
        return slimePlugin.getLoader(defaultWorldData.getDataSource()).listWorlds();
    }

    @Override
    public ISlimeWorld loadWorld(String worldName, World.Environment environment) {
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

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        slimePlugin.generateWorld(((SWMSlimeWorld) slimeWorld).getHandle());
    }

    @Override
    public void deleteWorld(Island island, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(island, environment);

        WorldData worldData = ConfigManager.getWorldConfig().getWorlds().get(worldName);

        if (worldData == null)
            return;

        SlimeLoader slimeLoader = slimePlugin.getLoader(worldData.getDataSource());

        SlimeUtils.unloadWorld(worldName, false).whenComplete((result, error) -> {
            if (error != null) {
                error.printStackTrace();
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        slimeLoader.deleteWorld(worldName);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
    }

    private WorldData buildDefaultWorldData() {
        WorldData worldData = new WorldData();

        worldData.setDataSource("file");
        worldData.setDifficulty(plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
        worldData.setLoadOnStartup(false);

        return worldData;
    }

}
