package com.bgsoftware.ssbslimeworldmanager.config;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;

import java.io.File;

public class SettingsManager {

    public final String dataSource;
    public final int unloadDelay;

    public SettingsManager(SlimeWorldModule module) {
        File file = new File(module.getModuleFolder(), "config.yml");

        if (!file.exists())
            module.saveResource("config.yml");

        CommentedConfiguration config = CommentedConfiguration.loadConfiguration(file);

        try {
            config.syncWithConfig(file, module.getResource("config.yml"));
        } catch (Exception error) {
            error.printStackTrace();
        }

        this.dataSource = config.getString("data-source");
        this.unloadDelay = config.getInt("unload-delay");
    }

}