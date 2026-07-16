package yemian;

public class banbenhao {
    // 客户端版本号
    private static final String CLIENT_VERSION = "3.0.0";
    
    // 服务器要求的最低版本号
    private static final String MIN_REQUIRED_VERSION = "3.0.0";
    
    /**
     * 获取客户端版本号
     * @return 版本号字符串
     */
    public static String getClientVersion() {
        return CLIENT_VERSION;
    }
    
    /**
     * 获取最低要求版本号
     * @return 最低要求版本号字符串
     */
    public static String getMinRequiredVersion() {
        return MIN_REQUIRED_VERSION;
    }
    
    /**
     * 验证版本号是否有效
     * @param version 待验证的版本号
     * @return 如果版本号有效返回true，否则返回false
     */
    public static boolean isValidVersion(String version) {
        // 简单的版本号格式验证
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        // 这里可以添加更复杂的版本号验证逻辑
        // 目前只做基本的非空检查
        return true;
    }
    
    /**
     * 检查客户端版本是否满足最低要求
     * @return 如果满足要求返回true，否则返回false
     */
    public static boolean isVersionCompatible() {
        // 简单的版本比较，实际项目中可能需要更复杂的版本比较逻辑
        // 确保客户端版本大于等于最低要求版本
        return CLIENT_VERSION.compareTo(MIN_REQUIRED_VERSION) >= 0;
    }
}
