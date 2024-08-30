package com.bgsoftware.ssbslimeworldmanager.swm.impl.swm;

import com.bgsoftware.ssbslimeworldmanager.api.DataSourceParams;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.google.common.base.Preconditions;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
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

    public SWMAdapter(SuperiorSkyblock plugin, DataSourceParams dataSource) {
        this.plugin = plugin;
        this.slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        Preconditions.checkState(this.slimePlugin != null, "SlimeWorldManager plugin does not exist");
        String dataSourceType = dataSource.getType().name().toLowerCase(Locale.ENGLISH);;
        this.slimeLoader = this.slimePlugin.getLoader(dataSourceType);
        Preconditions.checkState(this.slimeLoader != null, "Invalid data source: " + dataSourceType);
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
                    properties.setString(SlimeProperties.DIFFICULTY, plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
                    properties.setString(SlimeProperties.ENVIRONMENT, environment.name().toLowerCase(Locale.ENGLISH));

                    slimeWorld = new SWMSlimeWorld(slimePlugin.createEmptyWorld(slimeLoader, worldName, false, properties));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (IOException | CorruptedWorldException | NewerFormatException | WorldInUseException |
                     UnknownWorldException | WorldAlreadyExistsException exception) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred while trying to create or load world: " + worldName, exception);
            }
        }

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        slimePlugin.generateWorld(((SWMSlimeWorld) slimeWorld).getHandle());
    }

    @Override
    public boolean deleteWorld(Island island, Dimension dimension) {
        String worldName = SlimeUtils.getWorldName(island.getUniqueId(), dimension);

        if (Bukkit.unloadWorld(worldName, false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    slimeLoader.deleteWorld(worldName);
                } catch (UnknownWorldException ignored) {
                    // World was not saved yet, who cares.
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });

            return true;
        }

        return false;
    }

}
