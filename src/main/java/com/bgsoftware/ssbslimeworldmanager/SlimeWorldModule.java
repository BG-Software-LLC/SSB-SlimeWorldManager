package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.config.SettingsManager;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsCreationAlgorithm;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsProvider;
import com.bgsoftware.ssbslimeworldmanager.listeners.IslandsListener;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.commands.SuperiorCommand;
import com.bgsoftware.superiorskyblock.api.modules.ModuleLoadTime;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

public class SlimeWorldModule extends PluginModule {

    private static SlimeWorldModule instance;

    private SuperiorSkyblock plugin;
    private SettingsManager settingsManager;

    @Nullable
    private ISlimeAdapter slimeAdapter;
    private SlimeWorldsProvider slimeWorldsProvider;

    public SlimeWorldModule() {
        super("SlimeWorldIslands", "Ome_R");
        instance = this;
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        this.plugin = plugin;

        this.settingsManager = new SettingsManager(this);

        this.slimeAdapter = loadAdapter();
        if (this.slimeAdapter == null)
            throw new RuntimeException("Could not find SWM/ASWM adapter. Ensure that your data source is correct in the config.yml for SlimeWorldIslands.");

        loadWorldsProvider();

        loadCreationAlgorithm();
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {

    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        if (slimeAdapter == null)
            return;

        List<String> worlds;

        try {
            worlds = slimeAdapter.getSavedWorlds();
        } catch (IOException error) {
            error.printStackTrace();
            return;
        }

        // Save all the islands when the server shuts down
        for (String worldName : worlds) {
            if (SlimeUtils.isIslandWorldName(worldName) && Bukkit.getWorld(worldName) != null) {
                SlimeUtils.saveAndUnloadWorld(worldName);
            }
        }
    }

    @Override
    public Listener[] getModuleListeners(SuperiorSkyblock plugin) {
        return new Listener[]{new IslandsListener(this)};
    }

    @Nullable
    @Override
    public SuperiorCommand[] getSuperiorCommands(SuperiorSkyblock plugin) {
        return null;
    }

    @Nullable
    @Override
    public SuperiorCommand[] getSuperiorAdminCommands(SuperiorSkyblock plugin) {
        return null;
    }

    @Override
    public ModuleLoadTime getLoadTime() {
        return ModuleLoadTime.BEFORE_WORLD_CREATION;
    }

    public SettingsManager getSettings() {
        return settingsManager;
    }

    public ISlimeAdapter getSlimeAdapter() {
        return Objects.requireNonNull(slimeAdapter);
    }

    public SlimeWorldsProvider getSlimeWorldsProvider() {
        return slimeWorldsProvider;
    }

    public SuperiorSkyblock getPlugin() {
        return plugin;
    }

    public static SlimeWorldModule getModule() {
        return instance;
    }

    private ISlimeAdapter loadAdapter() {
        ISlimeAdapter slimeAdapter;

        try {
            Class.forName("com.infernalsuite.aswm.api.AdvancedSlimePaperAPI");
            slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.asp3.SWMAdapter");
        } catch (Throwable ignored) {
            try {
                Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
                slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.asp.SWMAdapter");
            } catch (Throwable ignored2) {
                try {
                    Class.forName("com.grinderwolf.swm.nms.world.AbstractSlimeNMSWorld");
                    slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.aswm.SWMAdapter");
                } catch (Throwable ignored3) {
                    slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.swm.SWMAdapter");
                }
            }
        }

        return slimeAdapter;
    }

    private void loadWorldsProvider() {
        slimeWorldsProvider = new SlimeWorldsProvider(this);
        plugin.getProviders().setWorldsProvider(this.slimeWorldsProvider);
    }

    private void loadCreationAlgorithm() {
        IslandCreationAlgorithm islandCreationAlgorithm = plugin.getGrid().getIslandCreationAlgorithm();
        plugin.getGrid().setIslandCreationAlgorithm(new SlimeWorldsCreationAlgorithm(this, islandCreationAlgorithm));
    }

    private ISlimeAdapter createAdapterInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (constructor.getParameterCount() == 2 && (constructor.getParameterTypes()[0].equals(SuperiorSkyblock.class) && constructor.getParameterTypes()[1].equals(DataSourceParams.class))) {
                    return (ISlimeAdapter) constructor.newInstance(this.plugin, settingsManager.dataSource);
                }
            }

            return (ISlimeAdapter) clazz.newInstance();
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

}
