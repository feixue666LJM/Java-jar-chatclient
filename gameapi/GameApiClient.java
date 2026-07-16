package gameapi;

public interface GameApiClient {
    String getName();

    String getDescription();

    String getApiToken();

    default boolean hasApiToken() {
        String token = getApiToken();
        return token != null && !token.trim().isEmpty();
    }
}
