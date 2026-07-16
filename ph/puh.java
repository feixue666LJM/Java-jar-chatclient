package ph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.swing.filechooser.FileNameExtensionFilter;

 // 导入ChatClient类

public class puh {
    // 定义一个回调接口用于处理图片消息显示
    public interface ImageMessageCallback {
        void onImageReceived(String fileName, String imageId);
    }
    
    // 存储图片块的缓存
    private static Map<String, ImageChunkReceiver> imageReceivers = new HashMap<>();
    
    // 图片块接收器类
    private static class ImageChunkReceiver {
        private String imageId;
        private int totalChunks;
        private Map<Integer, String> receivedChunks;
        private long lastUpdateTime;
        private String fileName;
        
        public ImageChunkReceiver(String imageId, int totalChunks) {
            this.imageId = imageId;
            this.totalChunks = totalChunks;
            this.receivedChunks = new HashMap<>();
            this.lastUpdateTime = System.currentTimeMillis();
            System.out.println("创建图片接收器: " + imageId + ", 总块数: " + totalChunks);
        }
        
        public boolean addChunk(int chunkIndex, String chunkData) {
            System.out.println("添加图片块: " + imageId + ", 块索引: " + chunkIndex + ", 数据长度: " + chunkData.length());
            receivedChunks.put(chunkIndex, chunkData);
            lastUpdateTime = System.currentTimeMillis();
            System.out.println("已接收块数: " + receivedChunks.size() + "/" + totalChunks);
            return receivedChunks.size() == totalChunks;
        }
        
        public String getCompleteImageData() {
            System.out.println("组装完整图片数据，总块数: " + receivedChunks.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < totalChunks; i++) {
                String chunk = receivedChunks.get(i);
                if (chunk != null) {
                    sb.append(chunk);
                    System.out.println("添加块 " + i + ": " + chunk.length() + " 字符");
                } else {
                    System.out.println("警告：缺少块 " + i);
                }
            }
            System.out.println("完整图片数据长度: " + sb.length());
            return sb.toString();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastUpdateTime > 30000; // 30秒超时
        }
        
        // 添加设置文件名的方法
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        // 添加获取文件名的方法
        public String getFileName() {
            return fileName;
        }
    }
    
    // 发送图片消息
    public static void sendImageMessage(JTextField inputField, java.io.PrintWriter out, String currentUsername, 
                                       long lastImageSendTime, long imageSendInterval, long maxImageSize,
                                       Runnable updateLastImageSendTime) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files (JPG, JPEG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // 检查文件大小
                long fileSize = selectedFile.length();
                if (fileSize > maxImageSize) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, 
                            "图片文件过大，不能超过20MB\n当前文件大小: " + (fileSize / (1024 * 1024)) + "MB", 
                            "错误", JOptionPane.ERROR_MESSAGE);
                    });
                    return;
                }
                
                // 检查发送频率
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastImageSendTime < imageSendInterval) {
                    long remainingTime = (imageSendInterval - (currentTime - lastImageSendTime)) / 1000;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, 
                            "发送图片过于频繁，每分钟只能发送一张图片\n请等待 " + (remainingTime + 1) + " 秒后再试", 
                            "提示", JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                
                // 更新最后发送时间
                updateLastImageSendTime.run();
                
                // 读取图片文件
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                
                // 分块处理大图片
                int chunkSize = 50000; // 每块50KB
                int totalChunks = (int) Math.ceil((double) base64Image.length() / chunkSize);
                String imageId = "img_" + System.currentTimeMillis();
                
                // 发送图片信息
                out.println("/image_info|" + imageId + "|" + totalChunks + "|" + selectedFile.getName());
                
                // 发送图片块
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, base64Image.length());
                    String chunkData = base64Image.substring(start, end);
                    out.println("/image_chunk|" + imageId + "|" + i + "|" + chunkData);
                }
                
                inputField.setText("");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "图片发送成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "发送图片失败: " + e.getMessage(), 
                                                "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }
    }
    
    // 处理接收到的图片信息
    public static void processImageInfo(String message, JTextArea logArea) {
        String[] parts = message.split("\\|", 4);
        if (parts.length == 4) {
            String imageId = parts[1];
            int totalChunks = Integer.parseInt(parts[2]);
            String fileName = parts[3];
            
            ImageChunkReceiver receiver = new ImageChunkReceiver(imageId, totalChunks);
            receiver.setFileName(fileName); // 设置文件名
            imageReceivers.put(imageId, receiver);
            
            SwingUtilities.invokeLater(() -> {
                logArea.append("[系统] 正在接收图片: " + fileName + " (" + totalChunks + " 块)\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append("[系统] 图片信息格式错误\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }
    
    // 处理接收到的图片块
    public static void processImageChunk(String message, JTextArea logArea, Map<String, String> imageStore, 
                                       ImageMessageCallback callback) {
        String[] parts = message.split("\\|", 4);
        if (parts.length == 4) {
            String imageId = parts[1];
            int chunkIndex = Integer.parseInt(parts[2]);
            String chunkData = parts[3];
            
            ImageChunkReceiver receiver = imageReceivers.get(imageId);
            if (receiver != null) {
                boolean isComplete = receiver.addChunk(chunkIndex, chunkData);
                if (isComplete) {
                    // 图片接收完成
                    String completeImageData = receiver.getCompleteImageData();
                    String fileName = receiver.getFileName(); // 现在可以正确获取文件名了
                    imageReceivers.remove(imageId);
                    
                    // 添加调试信息
                    System.out.println("图片接收完成: " + fileName + ", 数据长度: " + completeImageData.length());
                    
                    // 将图片数据存储到映射中
                    imageStore.put(imageId, completeImageData);
                    
                    SwingUtilities.invokeLater(() -> {
                        // 使用回调接口通知图片已接收
                        if (callback != null) {
                            callback.onImageReceived(fileName, imageId);
                        }
                    });
                }
            } else {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("[系统] 未找到图片接收器: " + imageId + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append("[系统] 图片块格式错误\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }
    
    // 显示图片
    private static void displayImage(String base64Image, JTextArea logArea) {
        try {
            System.out.println("开始显示图片，Base64数据长度: " + (base64Image != null ? base64Image.length() : "null"));
            
            // 检查数据是否为空
            if (base64Image == null || base64Image.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "图片数据为空", "错误", JOptionPane.ERROR_MESSAGE);
                });
                return;
            }
            
            // 显示前100个字符作为调试信息
            System.out.println("Base64数据前100字符: " + base64Image.substring(0, Math.min(100, base64Image.length())));
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            System.out.println("解码后的字节长度: " + imageBytes.length);
            
            ImageIcon imageIcon = new ImageIcon(imageBytes);
            System.out.println("图片尺寸: " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight());
            
            // 检查图片是否有效
            if (imageIcon.getIconWidth() <= 0 || imageIcon.getIconHeight() <= 0) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "无效的图片数据", "错误", JOptionPane.ERROR_MESSAGE);
                });
                return;
            }
            
            // 创建缩放后的图片（如果太大）
            Image img = imageIcon.getImage();
            int maxWidth = 400;
            int maxHeight = 300;
            int width = img.getWidth(null);
            int height = img.getHeight(null);
            System.out.println("原始图片尺寸: " + width + "x" + height);
            
            if (width > maxWidth || height > maxHeight) {
                double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);
                System.out.println("缩放图片到: " + newWidth + "x" + newHeight);
                img = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(img);
            }
            
            JFrame frame = new JFrame("查看图片");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLabel label = new JLabel(imageIcon);
            label.setToolTipText("图片已接收");
            frame.add(label);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            System.out.println("图片窗口已显示");
        } catch (Exception e) {
            System.err.println("显示图片失败: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "显示图片失败: " + e.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    // 清理过期的图片接收器
    public static void cleanupExpiredReceivers() {
        imageReceivers.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
