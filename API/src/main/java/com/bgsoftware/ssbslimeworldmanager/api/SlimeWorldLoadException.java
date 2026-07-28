package com.bgsoftware.ssbslimeworldmanager.api;

/**
 * Thrown when an island world could not be loaded from the data-source.
 * <p>
 * The most common cause on a network is that the world is still locked by another server that
 * has not released it yet - for example a server that crashed while the island was loaded.
 * Callers are expected to handle this and retry later instead of assuming the world exists.
 */
public class SlimeWorldLoadException extends RuntimeException {

    private final String worldName;

    public SlimeWorldLoadException(String worldName) {
        super("Failed to create or load island world \"" + worldName + "\". " +
                "It may be corrupted, or still locked by another server.");
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

}
