package booger;

import chatspeak.ChatClient;
import javax.swing.*;
import java.lang.reflect.Method;

/**
 * Minecraft <-> ChatClient bridge.
 *
 * Uses Class.forName with intermediary name (class_310)
 * to avoid Knot classloader remapping issues.
 * BoogerMod does NOT import any Minecraft class.
 */
public class MinecraftChatBridge {

    private static Object chatClientInstance;

    private static final String PUBLIC_CHANNEL_GROUP = "group_public";
    /**
     * Called from BoogerMod.onInitializeClient().
     * Waits 1.5s for game init, then gets the player name via
     * reflection on the Minecraft render thread.
     */
    public static void startDelayed() {
        new Thread(MinecraftChatBridge::delayedLaunch).start();
    }

    private static void delayedLaunch() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {}

        try {
            Class<?> mcClass = resolveMcClass();
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object session = mcClass.getMethod("getSession").invoke(mc);
            String playerName = (String) session.getClass()
                .getMethod("getUsername").invoke(session);

            // Switch to render thread via execute()
            Method executeMethod = mcClass.getMethod("execute", Runnable.class);
            String name = playerName; // effectively final for lambda
            executeMethod.invoke(mc, (Runnable) () -> launchChatClient(name));
        } catch (Exception e) {
            System.err.println("[booger] Error accessing Minecraft: " + e);
            e.printStackTrace();
        }
    }

    private static Class<?> resolveMcClass() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.class_310");
        } catch (ClassNotFoundException e) {
            return Class.forName("net.minecraft.client.MinecraftClient");
        }
    }

    public static void launchChatClient(String minecraftPlayerName) {
        launchChatClient(minecraftPlayerName, PUBLIC_CHANNEL_GROUP);
    }

    public static void launchChatClient(String minecraftPlayerName, String accountName) {
        SwingUtilities.invokeLater(() -> {
            try {
                String group = accountName != null ? accountName : PUBLIC_CHANNEL_GROUP;
                chatClientInstance = ChatClient.startWithAutoLogin(minecraftPlayerName, group);
                System.out.println("[booger] ChatClient launched for: " + minecraftPlayerName);
            } catch (Exception e) {
                System.err.println("[booger] Failed to launch ChatClient: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static Object getChatClientInstance() {
        return chatClientInstance;
    }
}
