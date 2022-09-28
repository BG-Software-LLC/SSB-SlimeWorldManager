package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.config.ConfigSettings;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsCreationAlgorithm;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsProvider;
import com.bgsoftware.ssbslimeworldmanager.listeners.IslandsListener;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.utils.SlimeUtils;
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

public final class SlimeWorldModule extends PluginModule {

    private static ConfigSettings configSettings;

    private ISlimeAdapter slimeAdapter;
    private SuperiorSkyblock plugin;

    private SlimeWorldsProvider slimeWorldsProvider;

    public SlimeWorldModule() {
        super("SlimeWorldIslands", "Ome_R");
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        this.plugin = plugin;

        if (!Bukkit.getPluginManager().isPluginEnabled("SlimeWorldManager"))
            throw new RuntimeException("SlimeWorldManager must be installed in order to use this module.");

        configSettings = new ConfigSettings(this);

        loadAdapter();

        if(slimeAdapter == null)
            throw new RuntimeException("Could not find SWM/ASWM adapter. Ensure that your data source is correct in the config.yml for SlimeWorldIslands.");

        loadWorldsProvider();

        loadCreationAlgorithm();
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {

    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        final List<String> worlds;

        try {
            worlds = slimeAdapter.getLoadedWorlds();
        } catch (IOException error) {
            error.printStackTrace();
            return;
        }



        // save all the islands when the server shuts down
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
        return ModuleLoadTime.AFTER_HANDLERS_LOADING;
    }

    public static ConfigSettings getConfigSettings() {
        return configSettings;
    }

    public ISlimeAdapter getSlimeAdapter() {
        return slimeAdapter;
    }

    public SlimeWorldsProvider getSlimeWorldsProvider() {
        return slimeWorldsProvider;
    }

    public SuperiorSkyblock getPlugin() {
        return plugin;
    }

    private void loadAdapter() {
        try {
            Class.forName("com.grinderwolf.swm.nms.world.AbstractSlimeNMSWorld");
            slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.aswm.SWMAdapter");
        } catch (Throwable error) {
            slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.swm.SWMAdapter");
        }
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
                if (constructor.getParameterCount() == 2 && (constructor.getParameterTypes()[0].equals(SuperiorSkyblock.class) && constructor.getParameterTypes()[1].equals(String.class))) {
                    return (ISlimeAdapter) constructor.newInstance(this.plugin, configSettings.getDataSource());
                }
            }

            return (ISlimeAdapter) clazz.newInstance();
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

}
