package gameapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Stores the optional game API token in the ChatSpeak config directory. */
public final class GameApiTokenConfig {
    private static final File CONFIG_FILE = new File(
            System.getProperty("user.home"), ".chatspeak/gameapi.properties");

    private GameApiTokenConfig() {
    }

    public static synchronized String getToken() {
        String token = System.getProperty("chatspeak.gameapi.token");
        if (token == null || token.trim().isEmpty()) {
            token = System.getenv("CHATSPEAK_GAME_API_TOKEN");
        }
        if (token == null || token.trim().isEmpty()) {
            Properties properties = new Properties();
            if (CONFIG_FILE.isFile()) {
                try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
                    properties.load(input);
                } catch (IOException ignored) {
                    return "";
                }
            }
            token = properties.getProperty("token", "");
        }
        return token == null ? "" : token.trim();
    }

    public static synchronized void saveToken(String token) throws IOException {
        String value = token == null ? "" : token.trim();
        File parent = CONFIG_FILE.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建游戏 API 配置目录: " + parent);
        }
        Properties properties = new Properties();
        properties.setProperty("token", value);
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Optional game API token");
        }
    }
}
