package gameapi.hphta;

import gameapi.GameApiTokenConfig;

public final class pdssd {
    private pdssd() {
    }

    public static String getApiToken() {
        return GameApiTokenConfig.getToken();
    }

    public static boolean hasApiToken() {
        return hasApiToken(getApiToken());
    }

    public static boolean hasApiToken(String token) {
        return token != null && !token.trim().isEmpty();
    }
}
