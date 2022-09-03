package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.listeners.IslandsListener;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.commands.SuperiorCommand;
import com.bgsoftware.superiorskyblock.api.modules.ModuleLoadTime;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;

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
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {

    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        slimeAdapter.unloadAllWorlds();
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

    private static ISlimeAdapter createAdapterInstance(String className) {
        try {
            return (ISlimeAdapter) Class.forName(className).newInstance();
        } catch (Exception error) {
            return null;
        }
    }

}
