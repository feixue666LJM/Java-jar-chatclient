package gameapi.Tencent;

import gameapi.GameApiClient;

public final class DeltaForce implements GameApiClient {
    private final String apiToken;

    public DeltaForce(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String getName() {
        return "DeltaForce";
    }

    @Override
    public String getDescription() {
        return "三角洲行动";
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }
}
