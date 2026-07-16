package ph

import java.awt.Dimension
import java.awt.Image
import java.awt.Toolkit
import java.util.Base64
import javax.swing.*

class phlk {
    companion object {
        /**
         * 显示图片
         * @param base64Image Base64编码的图片数据
         * @param imageName 图片名称
         */
        @JvmStatic
        fun displayImage(base64Image: String, imageName: String) {
            SwingUtilities.invokeLater {
                try {
                    println("准备显示图片: $imageName, 数据长度: ${base64Image.length}")
                    
                    // 检查数据是否为空
                    if (base64Image.isEmpty()) {
                        showError("图片数据为空")
                        return@invokeLater
                    }
                    
                    // 解码Base64图片数据
                    val imageBytes = Base64.getDecoder().decode(base64Image)
                    println("解码后图片字节长度: ${imageBytes.size}")
                    
                    if (imageBytes.isEmpty()) {
                        showError("解码后图片数据为空")
                        return@invokeLater
                    }
                    
                    // 创建ImageIcon
                    val originalIcon = ImageIcon(imageBytes)
                    println("图片尺寸: ${originalIcon.iconWidth}x${originalIcon.iconHeight}")
                    
                    // 检查图片是否有效
                    if (originalIcon.iconWidth <= 0 || originalIcon.iconHeight <= 0) {
                        showError("无效的图片数据")
                        return@invokeLater
                    }
                    
                    // 创建窗口显示图片
                    val frame = JFrame("查看图片 - $imageName")
                    frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    
                    // 获取屏幕尺寸
                    val screenSize = Toolkit.getDefaultToolkit().screenSize
                    val maxWidth = (screenSize.width * 0.8).toInt()
                    val maxHeight = (screenSize.height * 0.8).toInt()
                    
                    // 获取原始图片尺寸
                    val imgWidth = originalIcon.iconWidth
                    val imgHeight = originalIcon.iconHeight
                    
                    // 计算缩放比例
                    val scale = Math.min(maxWidth.toDouble() / imgWidth, maxHeight.toDouble() / imgHeight)
                        .coerceAtMost(1.0) // 不放大图片
                    
                    // 如果图片尺寸合适，直接使用原图
                    val displayIcon = if (scale >= 1.0) {
                        originalIcon
                    } else {
                        // 缩放图片
                        val newWidth = (imgWidth * scale).toInt()
                        val newHeight = (imgHeight * scale).toInt()
                        val scaledImage = originalIcon.image.getScaledInstance(
                            newWidth, newHeight, Image.SCALE_SMOOTH)
                        ImageIcon(scaledImage)
                    }
                    
                    // 创建标签显示图片
                    val imageLabel = JLabel(displayIcon)
                    imageLabel.horizontalAlignment = JLabel.CENTER
                    
                    // 添加到滚动面板
                    val scrollPane = JScrollPane(imageLabel)
                    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    
                    frame.add(scrollPane)
                    frame.pack()
                    
                    // 设置窗口位置居中
                    frame.setLocationRelativeTo(null)
                    
                    // 设置窗口最小尺寸
                    frame.minimumSize = Dimension(300, 200)
                    
                    // 显示窗口
                    frame.isVisible = true
                    println("图片显示窗口已打开")
                    
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                    showError("图片数据解码失败: ${e.message}\n这可能是因为数据不是有效的Base64编码")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("显示图片失败: ${e.message}")
                }
            }
        }
        
        /**
         * 显示错误信息
         * @param message 错误信息
         */
        private fun showError(message: String) {
            System.err.println("图片显示错误: $message")
            JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.ERROR_MESSAGE)
        }
    }
}