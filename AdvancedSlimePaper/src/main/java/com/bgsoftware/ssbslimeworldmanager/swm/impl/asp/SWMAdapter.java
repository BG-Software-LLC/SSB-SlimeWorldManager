package com.bgsoftware.ssbslimeworldmanager.swm.impl.asp;

import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;
import com.bgsoftware.ssbslimeworldmanager.api.EnumerateMap;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.config.SettingsManager;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.SlimePlugin;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class SWMAdapter implements ISlimeAdapter {

    private static final EnumerateMap<Dimension, SlimePropertyMap> PROPERTIES = new EnumerateMap<>();

    private final SuperiorSkyblock plugin;
    private final SlimePlugin slimePlugin;
    private final SlimeLoader slimeLoader;

    public SWMAdapter(SuperiorSkyblock plugin, DataSourceParams dataSource) {
        this.plugin = plugin;
        this.slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        Preconditions.checkState(this.slimePlugin != null, "SlimeWorldManager plugin does not exist");
        String dataSourceType = dataSource.getType().name().toLowerCase(Locale.ENGLISH);
        this.slimeLoader = this.slimePlugin.getLoader(dataSourceType);
        Preconditions.checkState(this.slimeLoader != null, "Invalid data source: " + dataSourceType);
        loadDefaultProperties();
    }

    @Override
    public List<String> getSavedWorlds() throws IOException {
        return slimeLoader.listWorlds();
    }

    @Override
    public ISlimeWorld createOrLoadWorld(String worldName, Dimension dimension) {
        ISlimeWorld slimeWorld = SlimeUtils.getSlimeWorld(worldName);

        if (slimeWorld == null) {
            try {
                if (slimeLoader.worldExists(worldName)) {
                    slimeWorld = new SWMSlimeWorld(slimePlugin.loadWorld(slimeLoader, worldName, false,
                            getPropertyMap(dimension)));
                } else {
                    slimeWorld = new SWMSlimeWorld(slimePlugin.createEmptyWorld(slimeLoader, worldName,
                            false, getPropertyMap(dimension)));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (IOException | CorruptedWorldException | NewerFormatException | WorldLockedException |
                     UnknownWorldException | WorldAlreadyExistsException exception) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred while trying to create or load world: " + worldName, exception);
            }
        }

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        slimePlugin.loadWorld(((SWMSlimeWorld) slimeWorld).handle());
    }

    @Override
    public boolean deleteWorld(Island island, Dimension dimension) {
        String worldName = SlimeUtils.getWorldName(island.getUniqueId(), dimension);

        if (Bukkit.unloadWorld(worldName, false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    slimeLoader.deleteWorld(worldName);
                } catch (UnknownWorldException ignored) {
                    // World was not saved yet, who cares.
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });

            return true;
        }

        return false;
    }

    private void loadDefaultProperties() {
        for (Dimension dimension : Dimension.values())
            getPropertyMap(dimension);
    }

    private SlimePropertyMap getPropertyMap(Dimension dimension) {
        return PROPERTIES.computeIfAbsent(dimension, d -> {
            // New dimension, let's set its default properties.
            SlimePropertyMap properties = new SlimePropertyMap();
            properties.setValue(SlimeProperties.DIFFICULTY,
                    plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
            properties.setValue(SlimeProperties.ENVIRONMENT,
                    dimension.getEnvironment().name().toLowerCase(Locale.ENGLISH));
            SettingsManager.Worlds.DimensionConfig dimensionConfig = plugin.getSettings().getWorlds().getDimensionConfig(dimension);
            if (dimensionConfig != null) {
                properties.setValue(SlimeProperties.DEFAULT_BIOME, dimensionConfig.getBiome().toLowerCase(Locale.ENGLISH));
                if (dimensionConfig instanceof SettingsManager.Worlds.End end && end.isDragonFight())
                    properties.setValue(SlimeProperties.DRAGON_BATTLE, true);
            }
            return properties;
        });
    }

}
