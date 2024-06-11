package com.bgsoftware.ssbslimeworldmanager.swm.impl.asp;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.utils.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.SlimePlugin;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class SWMAdapter implements ISlimeAdapter {

    private final SuperiorSkyblock plugin;
    private final SlimePlugin slimePlugin;
    private final SlimeLoader slimeLoader;

    public SWMAdapter(SuperiorSkyblock plugin, String dataSource) {
        this.plugin = plugin;
        this.slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        Preconditions.checkState(this.slimePlugin != null, "SlimeWorldManager plugin does not exist");
        this.slimeLoader = this.slimePlugin.getLoader(dataSource);
        Preconditions.checkState(this.slimeLoader != null, "Invalid data source: " + dataSource);
    }

    @Override
    public List<String> getSavedWorlds() throws IOException {
        return slimeLoader.listWorlds();
    }

    @Override
    public ISlimeWorld createOrLoadWorld(String worldName, World.Environment environment) {
        ISlimeWorld slimeWorld = SlimeUtils.getSlimeWorld(worldName);

        if (slimeWorld == null) {
            SlimePropertyMap properties = new SlimePropertyMap();

            try {
                if (slimeLoader.worldExists(worldName)) {
                    slimeWorld = new SWMSlimeWorld(slimePlugin.loadWorld(slimeLoader, worldName, false, properties));
                } else {
                    // set the default island properties accordingly
                    properties.setValue(SlimeProperties.DIFFICULTY, plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
                    properties.setValue(SlimeProperties.ENVIRONMENT, environment.name().toLowerCase(Locale.ENGLISH));

                    slimeWorld = new SWMSlimeWorld(slimePlugin.createEmptyWorld(slimeLoader, worldName, false, properties));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (IOException | CorruptedWorldException | NewerFormatException | WorldLockedException |
                     UnknownWorldException | WorldAlreadyExistsException exception) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred while trying to create or load world: " + worldName, exception);
            }
        }

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        slimePlugin.loadWorld(((SWMSlimeWorld) slimeWorld).handle());
    }

    @Override
    public void deleteWorld(Island island, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(island.getUniqueId(), environment);

        if (Bukkit.unloadWorld(worldName, false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    slimeLoader.deleteWorld(worldName);
                } catch (UnknownWorldException | IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

}
