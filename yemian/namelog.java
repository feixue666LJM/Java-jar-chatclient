package yemian;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class namelog {
    // 存储每个用户的消息发送记录
    private static Map<String, UserMessageTracker> userTrackers = new ConcurrentHashMap<>();
    
    // 用户昵称最大字节数
    private static final int MAX_NICKNAME_BYTES = 30;
    
    // 时间窗口和限制
    private static final long ONE_MINUTE = 60 * 1000; // 1分钟
    private static final int MAX_MESSAGES_PER_MINUTE = 5; // 每分钟最多5条消息
    
    private static final long THIRTY_MINUTES = 30 * 60 * 1000; // 30分钟
    private static final int MAX_MESSAGES_PER_30_MINUTES = 140; // 30分钟最多140条消息
    
    private static final long HALF_HOUR = 30 * 60 * 1000; // 半小时禁止时间
    private static final int MAX_DUPLICATE_MESSAGES = 2; // 最大重复消息数
    
    // 用户消息跟踪器类
    private static class UserMessageTracker {
        private String userId;
        private List<Long> messageTimestamps; // 所有消息时间戳
        private Map<String, Integer> messageCounts; // 消息内容计数
        private long muteUntil; // 禁言截止时间
        
        public UserMessageTracker(String userId) {
            this.userId = userId;
            this.messageTimestamps = new ArrayList<>();
            this.messageCounts = new HashMap<>();
            this.muteUntil = 0;
        }
        
        // 检查用户是否被禁言
        public boolean isMuted() {
            return System.currentTimeMillis() < muteUntil;
        }
        
        // 设置禁言
        public void setMute(long duration) {
            this.muteUntil = System.currentTimeMillis() + duration;
        }
        
        // 获取禁言剩余时间（毫秒）
        public long getMuteRemainingTime() {
            long now = System.currentTimeMillis();
            return now < muteUntil ? muteUntil - now : 0;
        }
    }
    
    // 验证昵称长度
    public static boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }
        
        try {
            byte[] nicknameBytes = nickname.getBytes("UTF-8");
            return nicknameBytes.length <= MAX_NICKNAME_BYTES;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 获取昵称最大字节数
    public static int getMaxNicknameBytes() {
        return MAX_NICKNAME_BYTES;
    }
    
    // 检查用户是否可以发送消息
    public static SendMessageResult canSendMessage(String userId, String message) {
        UserMessageTracker tracker = userTrackers.computeIfAbsent(userId, UserMessageTracker::new);
        
        // 检查是否被禁言
        if (tracker.isMuted()) {
            long remainingTime = tracker.getMuteRemainingTime();
            return new SendMessageResult(false, "您已被禁言，剩余时间: " + (remainingTime / 1000) + "秒");
        }
        
        long now = System.currentTimeMillis();
        
        // 清理过期的消息记录
        cleanupExpiredMessages(tracker, now);
        
        // 检查每分钟消息限制
        long oneMinuteAgo = now - ONE_MINUTE;
        long recentMessages = tracker.messageTimestamps.stream()
                .filter(timestamp -> timestamp > oneMinuteAgo)
                .count();
        
        if (recentMessages >= MAX_MESSAGES_PER_MINUTE) {
            return new SendMessageResult(false, "发送消息过于频繁，请稍后再试");
        }
        
        // 检查30分钟内消息总数限制
        long thirtyMinutesAgo = now - THIRTY_MINUTES;
        long messagesIn30Minutes = tracker.messageTimestamps.stream()
                .filter(timestamp -> timestamp > thirtyMinutesAgo)
                .count();
        
        if (messagesIn30Minutes >= MAX_MESSAGES_PER_30_MINUTES) {
            // 超过限制，禁言半小时
            tracker.setMute(HALF_HOUR);
            return new SendMessageResult(false, "30分钟内发送消息超过140条，已被禁言半小时");
        }
        
        // 检查重复消息（语音消息跳过重复检测）
        if (!"[语音消息]".equals(message)) {
            int messageCount = tracker.messageCounts.getOrDefault(message, 0);
            if (messageCount >= MAX_DUPLICATE_MESSAGES) {
                return new SendMessageResult(false, "禁止重复发送相同消息");
            }
            // 记录消息计数（非语音消息）
            tracker.messageCounts.put(message, messageCount + 1);
        }
        
        // 记录消息时间戳（所有消息）
        tracker.messageTimestamps.add(now);
        
        return new SendMessageResult(true, "允许发送");
    }
    
    // 清理过期的消息记录
    private static void cleanupExpiredMessages(UserMessageTracker tracker, long now) {
        // 清理时间戳
        long thirtyMinutesAgo = now - THIRTY_MINUTES;
        tracker.messageTimestamps.removeIf(timestamp -> timestamp < thirtyMinutesAgo);
        
        // 清理消息计数
        Iterator<Map.Entry<String, Integer>> iterator = tracker.messageCounts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            // 如果该消息在30分钟内没有出现，则移除
            boolean recentMessageExists = tracker.messageTimestamps.stream()
                    .filter(timestamp -> timestamp > thirtyMinutesAgo)
                    .findFirst()
                    .isPresent();
            
            if (!recentMessageExists) {
                iterator.remove();
            }
        }
    }
    
    // 发送消息结果类
    public static class SendMessageResult {
        private boolean allowed;
        private String message;
        
        public SendMessageResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
