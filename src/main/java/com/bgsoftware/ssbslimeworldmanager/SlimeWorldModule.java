package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.config.SettingsManager;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsCreationAlgorithm;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsProvider;
import com.bgsoftware.ssbslimeworldmanager.listeners.IslandsListener;
import com.bgsoftware.ssbslimeworldmanager.listeners.SaveTriggersListener;
import com.bgsoftware.ssbslimeworldmanager.listeners.WorldsListener;
import com.bgsoftware.ssbslimeworldmanager.providers.ProvidersManager;
import com.bgsoftware.ssbslimeworldmanager.save.IslandWorldsSaveService;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.commands.SuperiorCommand;
import com.bgsoftware.superiorskyblock.api.modules.ModuleLoadTime;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SlimeWorldModule extends PluginModule {

    private static SlimeWorldModule instance;

    private SuperiorSkyblock plugin;
    private SettingsManager settingsManager;
    private ProvidersManager providersManager;

    @Nullable
    private ISlimeAdapter slimeAdapter;
    private SlimeWorldsProvider slimeWorldsProvider;
    private IslandWorldsSaveService saveService;

    public SlimeWorldModule() {
        super("SlimeWorldIslands", "Ome_R");
        instance = this;
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        this.plugin = plugin;

        this.settingsManager = new SettingsManager(this);
        this.providersManager = new ProvidersManager(this);

        this.slimeAdapter = loadAdapter();
        if (this.slimeAdapter == null)
            throw new RuntimeException("Could not find SWM/ASWM adapter. Ensure that your data source is correct in the config.yml for SlimeWorldIslands.");

        loadWorldsProvider();

        loadCreationAlgorithm();

        this.saveService = new IslandWorldsSaveService(this);
        this.saveService.start();

        this.providersManager.loadHooks();
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {

    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        if (this.saveService != null)
            this.saveService.stop();

        if (slimeAdapter == null)
            return;

        // Save all the islands when the server shuts down.
        // We iterate the loaded worlds instead of the saved ones, so islands that were never
        // written to the data-source before are saved as well.
        List<String> loadedIslandWorlds = new LinkedList<>();
        for (World world : Bukkit.getWorlds()) {
            if (SlimeUtils.isIslandsWorld(world.getName()) || SlimeUtils.isIslandWorldName(world.getName()))
                loadedIslandWorlds.add(world.getName());
        }

        for (String worldName : loadedIslandWorlds)
            SlimeUtils.saveAndUnloadWorld(worldName);
    }

    @Override
    public Listener[] getModuleListeners(SuperiorSkyblock plugin) {
        return new Listener[]{new IslandsListener(this), new WorldsListener(this), new SaveTriggersListener(this)};
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

    public ProvidersManager getProviders() {
        return providersManager;
    }

    public ISlimeAdapter getSlimeAdapter() {
        return Objects.requireNonNull(slimeAdapter);
    }

    public SlimeWorldsProvider getSlimeWorldsProvider() {
        return slimeWorldsProvider;
    }

    public IslandWorldsSaveService getSaveService() {
        return saveService;
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
            if (isClassLoaded("com.infernalsuite.asp.api.AdvancedSlimePaperAPI")) {
                slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.asp4.SWMAdapter");
            } else if (isClassLoaded("com.infernalsuite.aswm.api.AdvancedSlimePaperAPI")) {
                slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.asp3.SWMAdapter");
            } else if (isClassLoaded("com.infernalsuite.aswm.api.SlimePlugin")) {
                slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.asp.SWMAdapter");
            } else if (isClassLoaded("com.grinderwolf.swm.nms.world.AbstractSlimeNMSWorld")) {
                slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.aswm.SWMAdapter");
            } else {
                slimeAdapter = createAdapterInstance("com.bgsoftware.ssbslimeworldmanager.swm.impl.swm.SWMAdapter");
            }
        } catch (Throwable error) {
            // An unexpected error occurred, we want to throw it again.
            // However, in case of NoClassDefFoundError, this can happen if no adapter was found - in this case,
            // we only want to return null and let the module abort.
            // https://github.com/BG-Software-LLC/SSB-SlimeWorldManager/issues/93
            if (error instanceof NoClassDefFoundError) {
                slimeAdapter = null;
            } else {
                throw error;
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

    private static boolean isClassLoaded(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException error) {
            return false;
        }
    }

}
