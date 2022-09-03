package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsCreationAlgorithm;
import com.bgsoftware.ssbslimeworldmanager.hook.SlimeWorldsProvider;
import com.bgsoftware.ssbslimeworldmanager.listeners.IslandsListener;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SlimeWorldModule extends PluginModule {

    private ISlimeAdapter slimeAdapter;
    private SuperiorSkyblock plugin;

    public SlimeWorldModule() {
        super("SlimeWorldIslands", "Ome_R");
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        this.plugin = plugin;

        if (!Bukkit.getPluginManager().isPluginEnabled("SlimeWorldManager"))
            throw new RuntimeException("SlimeWorldManager must be installed in order to use this module.");

        loadAdapter();

        plugin.getProviders().setWorldsProvider(new SlimeWorldsProvider(this));

        IslandCreationAlgorithm islandCreationAlgorithm = plugin.getGrid().getIslandCreationAlgorithm();
        plugin.getGrid().setIslandCreationAlgorithm(new SlimeWorldsCreationAlgorithm(this, islandCreationAlgorithm));
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {

    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        List<String> worlds;

        try {
            worlds = slimeAdapter.getLoadedWorlds();
        } catch (IOException error) {
            error.printStackTrace();
            return;
        }

        List<CompletableFuture<Boolean>> unloadWorldTasks = new ArrayList<>(worlds.size());

        for (String worldName : worlds) {
            if (SlimeUtils.isIslandWorldName(worldName) && Bukkit.getWorld(worldName) != null)
                unloadWorldTasks.add(SlimeUtils.unloadWorld(worldName, true));
        }

        // Wait for all the tasks to complete.
        CompletableFuture.allOf(unloadWorldTasks.toArray(new CompletableFuture[0])).join();
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

    public ISlimeAdapter getSlimeAdapter() {
        return slimeAdapter;
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

    private ISlimeAdapter createAdapterInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].equals(SuperiorSkyblock.class))
                    return (ISlimeAdapter) constructor.newInstance(this.plugin);
            }

            return (ISlimeAdapter) clazz.newInstance();
        } catch (Exception error) {
            return null;
        }
    }

}
