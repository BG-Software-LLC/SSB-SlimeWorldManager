package com.bgsoftware.ssbslimeworldmanager.config;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;
import com.bgsoftware.ssbslimeworldmanager.save.SaveReason;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

public class SettingsManager {

    public final DataSourceParams dataSource;
    public final int unloadDelay;
    public final boolean teleportBackToIsland;
    public final boolean preloadIslandWorldsOnJoin;

    public final boolean autoSaveEnabled;
    public final long autoSaveInterval;
    public final long autoSaveMinDelay;
    public final int autoSaveSavesPerCycle;

    private final Set<SaveReason> enabledSaveTriggers = EnumSet.noneOf(SaveReason.class);

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
        this.preloadIslandWorldsOnJoin = config.getBoolean("preload-island-worlds-on-join", true);

        this.autoSaveEnabled = config.getBoolean("auto-save.enabled", true);
        this.autoSaveInterval = Math.max(config.getLong("auto-save.interval", 300L), 0L);
        this.autoSaveMinDelay = Math.max(config.getLong("auto-save.min-delay", 60L), 0L);
        this.autoSaveSavesPerCycle = Math.max(config.getInt("auto-save.saves-per-cycle", 1), 1);

        for (SaveReason saveReason : SaveReason.values()) {
            if (saveReason.isTrigger() && config.getBoolean("auto-save.triggers." + saveReason.getConfigKey(), true))
                this.enabledSaveTriggers.add(saveReason);
        }
    }

    public boolean isSaveTriggerEnabled(SaveReason saveReason) {
        return this.enabledSaveTriggers.contains(saveReason);
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