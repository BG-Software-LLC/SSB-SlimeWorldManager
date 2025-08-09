package com.bgsoftware.ssbslimeworldmanager.swm.impl.asp4;

import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;
import com.bgsoftware.ssbslimeworldmanager.api.EnumerateMap;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.api.SWMAdapterLoadException;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.config.SettingsManager;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.world.properties.type.SlimePropertyBoolean;
import com.infernalsuite.asp.loaders.api.APILoader;
import com.infernalsuite.asp.loaders.file.FileLoader;
import com.infernalsuite.asp.loaders.mongo.MongoLoader;
import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class SWMAdapter implements ISlimeAdapter {

    private static final EnumerateMap<Dimension, SlimePropertyMap> PROPERTIES = new EnumerateMap<>();

    private static final SlimePropertyBoolean SAVE_POI_PROPERTY = SlimePropertyBoolean.create("savePOI", false);
    private static final SlimePropertyBoolean SAVE_BLOCK_TICKS_PROPERTY = SlimePropertyBoolean.create("saveBlockTicks", false);
    private static final SlimePropertyBoolean SAVE_FLUID_TICKS_PROPERTY = SlimePropertyBoolean.create("saveFluidTicks", false);

    private final SuperiorSkyblock plugin;

    private final SlimeLoader slimeLoader;

    public SWMAdapter(SuperiorSkyblock plugin, DataSourceParams dataSource) throws SWMAdapterLoadException {
        this.plugin = plugin;
        this.slimeLoader = createSlimeLoader(dataSource);
        loadDefaultProperties();
    }

    private static SlimeLoader createSlimeLoader(DataSourceParams dataSourceParams) throws SWMAdapterLoadException {
        switch (dataSourceParams.getType()) {
            case MYSQL: {
                try {
                    DataSourceParams.MySQL mysqlParams = (DataSourceParams.MySQL) dataSourceParams;
                    return new MysqlLoader(mysqlParams.url, mysqlParams.host, mysqlParams.port, mysqlParams.database,
                            mysqlParams.useSSL, mysqlParams.username, mysqlParams.password);
                } catch (Throwable error) {
                    throw new SWMAdapterLoadException("Failed to connect to MySQL", error);
                }
            }
            case MONGODB: {
                try {
                    DataSourceParams.MongoDB mongoDBParams = (DataSourceParams.MongoDB) dataSourceParams;
                    return new MongoLoader(mongoDBParams.database, mongoDBParams.collection, mongoDBParams.username, mongoDBParams.password,
                            mongoDBParams.auth, mongoDBParams.host, mongoDBParams.port, mongoDBParams.url);
                } catch (Throwable error) {
                    throw new SWMAdapterLoadException("Failed to connect to MongoDB", error);
                }
            }
            case API: {
                try {
                    DataSourceParams.API apiParams = (DataSourceParams.API) dataSourceParams;
                    return new APILoader(apiParams.uri, apiParams.username, apiParams.token,
                            apiParams.ignoreSSLCertificate);
                } catch (Throwable error) {
                    throw new SWMAdapterLoadException("Failed to connect to API", error);
                }
            }
            case FILE: {
                try {
                    DataSourceParams.File fileParams = (DataSourceParams.File) dataSourceParams;
                    return new FileLoader(new File(fileParams.path));
                } catch (Throwable error) {
                    throw new SWMAdapterLoadException("Failed to find worlds folder", error);
                }
            }
        }

        throw new SWMAdapterLoadException("Invalid data source type: " + dataSourceParams.getType());
    }

    @Override
    public List<String> getSavedWorlds() throws IOException {
        return this.slimeLoader.listWorlds();
    }

    @Override
    public ISlimeWorld createOrLoadWorld(String worldName, Dimension dimension) {
        ISlimeWorld slimeWorld = SlimeUtils.getSlimeWorld(worldName);

        if (slimeWorld == null) {
            try {
                if (this.slimeLoader.worldExists(worldName)) {
                    slimeWorld = new SWMSlimeWorld(AdvancedSlimePaperAPI.instance().readWorld(
                            this.slimeLoader, worldName, false, getPropertyMap(dimension)));
                } else {
                    slimeWorld = new SWMSlimeWorld(AdvancedSlimePaperAPI.instance().createEmptyWorld(
                            worldName, false, getPropertyMap(dimension), this.slimeLoader));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (IOException | CorruptedWorldException | NewerFormatException | UnknownWorldException exception) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred while trying to create or load world: " + worldName, exception);
            }
        }

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        AdvancedSlimePaperAPI.instance().loadWorld(((SWMSlimeWorld) slimeWorld).handle(), false);
    }

    @Override
    public boolean deleteWorld(Island island, Dimension dimension) {
        String worldName = SlimeUtils.getWorldName(island.getUniqueId(), dimension);

        if (Bukkit.unloadWorld(worldName, false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    this.slimeLoader.deleteWorld(worldName);
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
            properties.setValue(SAVE_POI_PROPERTY, true);
            properties.setValue(SAVE_BLOCK_TICKS_PROPERTY, true);
            properties.setValue(SAVE_FLUID_TICKS_PROPERTY, true);
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
