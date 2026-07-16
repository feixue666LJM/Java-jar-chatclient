package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import java.util.function.Consumer;

/**
 * Compile-only stub.
 * Real Fabric API uses Event<Consumer<MinecraftClient>>,
 * but we use Consumer<Object> to avoid Knot classloader
 * failing on the Yarn-mapped MinecraftClient during lambda bootstrap.
 * Type erasure makes both forms bytecode-compatible at runtime.
 */
public final class ClientLifecycleEvents {
    public static final Event<Consumer<Object>> CLIENT_STARTED = new Event<>();
}