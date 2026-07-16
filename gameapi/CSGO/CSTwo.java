package gameapi.CSGO;

import gameapi.GameApiClient;

public final class CSTwo implements GameApiClient {
    private final String apiToken;

    public CSTwo(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String getName() {
        return "CSTwo";
    }

    @Override
    public String getDescription() {
        return "cs2";
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }
}
