package com.bgsoftware.ssbslimeworldmanager.save;

public enum SaveReason {

    /**
     * The world was saved by the interval-based auto save task.
     */
    AUTO_SAVE("auto-save"),

    /**
     * A player walked out of the area of the island.
     */
    ISLAND_LEAVE("island-leave"),

    /**
     * The last player of the island's world left it.
     */
    WORLD_EMPTY("world-empty"),

    /**
     * An upgrade was purchased for the island.
     */
    ISLAND_UPGRADE("island-upgrade"),

    /**
     * A schematic was pasted into the island.
     */
    SCHEMATIC_PASTE("schematic-paste"),

    /**
     * The biome of the island was changed.
     */
    BIOME_CHANGE("biome-change"),

    /**
     * The save was requested by another plugin or by the module itself, and therefore
     * it cannot be disabled by the configuration.
     */
    MANUAL("manual");

    private final String configKey;

    SaveReason(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    /**
     * Whether this reason can be toggled in the `auto-save.triggers` section of the config.
     */
    public boolean isTrigger() {
        return this != AUTO_SAVE && this != MANUAL;
    }

}
