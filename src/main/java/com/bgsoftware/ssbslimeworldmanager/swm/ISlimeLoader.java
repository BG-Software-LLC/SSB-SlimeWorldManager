package com.bgsoftware.ssbslimeworldmanager.swm;

import java.io.IOException;

public interface ISlimeLoader {

    void saveWorld(String worldName, byte[] data) throws IOException;

}
