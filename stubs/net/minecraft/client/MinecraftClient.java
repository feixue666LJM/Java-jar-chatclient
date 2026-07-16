package net.minecraft.client;

/**
 * Compile-only stub for MinecraftClient.
 * Real class is provided by the game jar at runtime.
 */
public class MinecraftClient {
    private static final MinecraftClient instance = new MinecraftClient();
    public static MinecraftClient getInstance() { return instance; }
    public Session getSession() { return new Session(); }
    public void execute(Runnable task) { task.run(); }

    public static class Session {
        public String getUsername() { return "Player"; }
    }
}