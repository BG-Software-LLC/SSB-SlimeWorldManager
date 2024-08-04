package com.bgsoftware.ssbslimeworldmanager.swm.impl.asp;

import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorld;

public record SWMSlimeWorld(SlimeWorld handle) implements ISlimeWorld {

    @Override
    public String getName() {
        return handle.getName();
    }

}
