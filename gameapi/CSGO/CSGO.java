package gameapi.CSGO;

import gameapi.GameApiClient;

public final class CSGO implements GameApiClient {
    private final String apiToken;

    public CSGO(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String getName() {
        return "CSGO";
    }

    @Override
    public String getDescription() {
        return "csgo";
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }
}
