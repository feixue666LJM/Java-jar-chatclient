package gameapi;

import gameapi.hphta.pdssd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameApiLoader {
    private static final String[] CLIENT_CLASS_NAMES = {
            "gameapi.Tencent.GameForPeace",
            "gameapi.Tencent.HonorOfKings",
            "gameapi.Tencent.DeltaForce",
            "gameapi.CSGO.CSGO",
            "gameapi.CSGO.CSTwo",
            "gameapi.flash.Flashing"
    };
    private static final List<GameApiClient> CLIENTS = new ArrayList<>();
    private static boolean loaded = false;

    private GameApiLoader() {
    }

    public static synchronized List<GameApiClient> loadIfConfigured() {
        if (loaded) {
            return getLoadedClients();
        }

        String token = pdssd.getApiToken();
        if (!pdssd.hasApiToken(token)) {
            return Collections.emptyList();
        }

        for (String className : CLIENT_CLASS_NAMES) {
            CLIENTS.add(createClient(className, token));
        }
        loaded = true;
        return getLoadedClients();
    }

    private static GameApiClient createClient(String className, String token) {
        try {
            Class<?> type = Class.forName(className);
            return (GameApiClient) type.getConstructor(String.class).newInstance(token);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load game API: " + className, e);
        }
    }

    public static synchronized List<GameApiClient> getLoadedClients() {
        return Collections.unmodifiableList(CLIENTS);
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }
}
