package com.bgsoftware.ssbslimeworldmanager.swm;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface ISlimeWorld {

    String getName();

    ISlimeLoader getLoader();

    CompletableFuture<byte[]> serialize() throws IOException;

}
