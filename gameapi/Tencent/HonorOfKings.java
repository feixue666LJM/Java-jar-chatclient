package gameapi.Tencent;

import gameapi.GameApiClient;

public final class HonorOfKings implements GameApiClient {
    private final String apiToken;

    public HonorOfKings(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String getName() {
        return "HonorOfKings";
    }

    @Override
    public String getDescription() {
        return "王者荣耀";
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }
}
