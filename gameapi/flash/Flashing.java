package gameapi.flash;

import gameapi.GameApiClient;

public final class Flashing implements GameApiClient {
    private final String apiToken;

    public Flashing(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String getName() {
        return "Flashing";
    }

    @Override
    public String getDescription() {
        return "flash游戏";
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }
}
