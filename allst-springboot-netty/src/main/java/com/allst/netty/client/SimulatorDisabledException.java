package com.allst.netty.client;

public final class SimulatorDisabledException extends RuntimeException {

    public SimulatorDisabledException() {
        super("device simulator is disabled");
    }
}
