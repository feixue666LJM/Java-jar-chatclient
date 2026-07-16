package gameapi.Tencent;

import gameapi.GameApiClient;

public final class GameForPeace implements GameApiClient {
    private final String apiToken;

    public GameForPeace(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String getName() {
        return "GameForPeace";
    }

    @Override
    public String getDescription() {
        return "和平精英";
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }
}
