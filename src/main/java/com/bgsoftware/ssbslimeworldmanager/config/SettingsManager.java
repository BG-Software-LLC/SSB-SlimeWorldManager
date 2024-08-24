package com.bgsoftware.ssbslimeworldmanager.config;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;

import java.io.File;

public class SettingsManager {

    public final DataSourceParams dataSource;
    public final int unloadDelay;
    public final boolean teleportBackToIsland;

    public SettingsManager(SlimeWorldModule module) {
        File file = new File(module.getModuleFolder(), "config.yml");

        if (!file.exists())
            module.saveResource("config.yml");

        CommentedConfiguration config = CommentedConfiguration.loadConfiguration(file);
        convertData(config);

        try {
            config.syncWithConfig(file, module.getResource("config.yml"));
        } catch (Exception error) {
            error.printStackTrace();
        }

        this.dataSource = DataSourceParams.parse(config.getConfigurationSection("data-source"));
        this.unloadDelay = config.getInt("unload-delay");
        this.teleportBackToIsland = config.getBoolean("teleport-back-to-island", true);
    }

    private static void convertData(CommentedConfiguration config) {
        if (config.isString("data-source")) {
            String dataSourceType = config.getString("data-source");
            config.set("data-source", null);
            config.set("data-source.type", dataSourceType);
            config.set("data-source.file.path", "slime_worlds");
        }
    }

}