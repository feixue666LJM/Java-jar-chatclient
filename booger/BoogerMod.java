package booger;

import net.fabricmc.api.ClientModInitializer;

/**
 * booger — Minecraft 1.20.1 Fabric mod
 *
 * No direct references to any Minecraft class!
 * All MC interaction is done via MinecraftChatBridge using reflection.
 */
public class BoogerMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MinecraftChatBridge.startDelayed();
    }
}