package yemian;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/** Shared server endpoint configuration for login and the main chat window. */
public final class ServerConfig {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 22233;
    private static final File CONFIG_FILE = new File(
            System.getProperty("user.home"), ".chatspeak/server.properties");

    private ServerConfig() {
    }

    public static synchronized String getHost() {
        return decodeAddressInput(getConfiguredHost());
    }

    public static synchronized String getDisplayHost() {
        String configured = getConfiguredHost();
        return isValidIpv4(configured) ? encodeIpv4(configured) : configured;
    }

    private static String getConfiguredHost() {
        String configured = System.getProperty("chatspeak.server.host");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("CHATSPEAK_SERVER_HOST");
        }
        if (configured == null || configured.trim().isEmpty()) {
            configured = loadProperties().getProperty("host", DEFAULT_HOST);
        }
        return configured.trim();
    }

    public static synchronized int getPort() {
        String configured = System.getProperty("chatspeak.server.port");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("CHATSPEAK_SERVER_PORT");
        }
        if (configured == null || configured.trim().isEmpty()) {
            configured = loadProperties().getProperty("port", String.valueOf(DEFAULT_PORT));
        }
        try {
            int port = Integer.parseInt(configured.trim());
            return port >= 1 && port <= 65535 ? port : DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            return DEFAULT_PORT;
        }
    }

    public static synchronized void save(String host, int port) throws IOException {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("服务器地址不能为空");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("端口必须在1到65535之间");
        }
        String input = host.trim();
        String resolvedHost = decodeAddressInput(input);
        if (input.matches("[0-9.]+") && !isValidIpv4(resolvedHost)) {
            throw new IllegalArgumentException("数字IP格式无效");
        }
        String storedHost = isValidIpv4(resolvedHost) ? encodeIpv4(resolvedHost) : input;
        File parent = CONFIG_FILE.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建配置目录: " + parent);
        }
        Properties properties = new Properties();
        properties.setProperty("host", storedHost);
        properties.setProperty("port", String.valueOf(port));
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "ChatSpeak server endpoint");
        }
    }

    public static String decodeAddressInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("服务器地址不能为空");
        }
        String value = input.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        boolean looksLikeCode = lower.matches("[a-iz]{1,3}(\\.[a-iz]{1,3}){3}");
        if (!looksLikeCode) {
            return value;
        }
        if (!value.equals(lower)) {
            throw new IllegalArgumentException("IP替代代码必须全部使用小写字母");
        }
        StringBuilder decoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '.') {
                decoded.append(ch);
            } else if (ch == 'z') {
                decoded.append('0');
            } else {
                decoded.append((char) ('1' + ch - 'a'));
            }
        }
        String address = decoded.toString();
        if (!isValidIpv4(address)) {
            throw new IllegalArgumentException("IP替代代码转换后的地址无效");
        }
        return address;
    }

    public static String encodeIpv4(String address) {
        if (!isValidIpv4(address)) {
            throw new IllegalArgumentException("只能编码有效的IPv4地址");
        }
        StringBuilder encoded = new StringBuilder(address.length());
        for (int i = 0; i < address.length(); i++) {
            char ch = address.charAt(i);
            if (ch == '.') {
                encoded.append(ch);
            } else if (ch == '0') {
                encoded.append('z');
            } else {
                encoded.append((char) ('a' + ch - '1'));
            }
        }
        return encoded.toString();
    }

    private static boolean isValidIpv4(String address) {
        if (address == null || !address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            return false;
        }
        String[] parts = address.split("\\.");
        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        if (!CONFIG_FILE.isFile()) {
            return properties;
        }
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException ignored) {
            // Use defaults when the optional configuration file is unreadable.
        }
        return properties;
    }
}
