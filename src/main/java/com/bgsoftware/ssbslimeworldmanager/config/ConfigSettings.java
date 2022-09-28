package com.bgsoftware.ssbslimeworldmanager.config;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class ConfigSettings {

    private final String dataSource;
    private final int unloadDelay;

    public ConfigSettings(SlimeWorldModule module) {
        File file = new File(module.getModuleFolder(), "config.yml");

        if(!file.exists())
            module.saveResource("config.yml");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.dataSource = config.getString("settings.data_source");
        this.unloadDelay = config.getInt("settings.unload_delay");
    }

    public String getDataSource() {
        return dataSource;
    }

    public int getUnloadDelay() {
        return unloadDelay;
    }
}