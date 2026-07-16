package ph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Base64;

public class lookph {
    
    /**
     * 显示图片
     * @param base64Image Base64编码的图片数据
     * @param imageName 图片名称
     */
    public static void displayImage(String base64Image, String imageName) {
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("准备显示图片: " + imageName + ", 数据长度: " + (base64Image != null ? base64Image.length() : "null"));
                
                // 检查数据是否为空
                if (base64Image == null || base64Image.isEmpty()) {
                    showError("图片数据为空");
                    return;
                }
                
                // 解码Base64图片数据
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                System.out.println("解码后图片字节长度: " + imageBytes.length);
                
                if (imageBytes.length == 0) {
                    showError("解码后图片数据为空");
                    return;
                }
                
                // 创建ImageIcon
                ImageIcon originalIcon = new ImageIcon(imageBytes);
                System.out.println("图片尺寸: " + originalIcon.getIconWidth() + "x" + originalIcon.getIconHeight());
                
                // 检查图片是否有效
                if (originalIcon.getIconWidth() <= 0 || originalIcon.getIconHeight() <= 0) {
                    showError("无效的图片数据");
                    return;
                }
                
                // 创建窗口显示图片
                JFrame frame = new JFrame("查看图片 - " + imageName);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                
                // 获取屏幕尺寸
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int maxWidth = (int) (screenSize.width * 0.8);
                int maxHeight = (int) (screenSize.height * 0.8);
                
                // 获取原始图片尺寸
                int imgWidth = originalIcon.getIconWidth();
                int imgHeight = originalIcon.getIconHeight();
                
                // 计算缩放比例
                double scale = Math.min((double) maxWidth / imgWidth, (double) maxHeight / imgHeight);
                scale = Math.min(scale, 1.0); // 不放大图片
                
                // 如果图片尺寸合适，直接使用原图
                ImageIcon displayIcon;
                if (scale >= 1.0) {
                    displayIcon = originalIcon;
                } else {
                    // 缩放图片
                    int newWidth = (int) (imgWidth * scale);
                    int newHeight = (int) (imgHeight * scale);
                    Image scaledImage = originalIcon.getImage().getScaledInstance(
                        newWidth, newHeight, Image.SCALE_SMOOTH);
                    displayIcon = new ImageIcon(scaledImage);
                }
                
                // 创建标签显示图片
                JLabel imageLabel = new JLabel(displayIcon);
                imageLabel.setHorizontalAlignment(JLabel.CENTER);
                
                // 添加到滚动面板
                JScrollPane scrollPane = new JScrollPane(imageLabel);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                frame.add(scrollPane);
                frame.pack();
                
                // 设置窗口位置居中
                frame.setLocationRelativeTo(null);
                
                // 设置窗口最小尺寸
                frame.setMinimumSize(new Dimension(300, 200));
                
                // 显示窗口
                frame.setVisible(true);
                System.out.println("图片显示窗口已打开");
                
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                showError("图片数据解码失败: " + e.getMessage() + "\n这可能是因为数据不是有效的Base64编码");
            } catch (Exception e) {
                e.printStackTrace();
                showError("显示图片失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 显示错误信息
     * @param message 错误信息
     */
    private static void showError(String message) {
        System.err.println("图片显示错误: " + message);
        JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
