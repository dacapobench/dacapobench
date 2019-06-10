package org.dacapo.kafka;

import java.util.concurrent.CountDownLatch;

public abstract class Initializer {
    public abstract void initialize() throws Exception;
}
