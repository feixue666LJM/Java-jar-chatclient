package yemian;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class twoyemian extends JFrame {
    private JComboBox<String> accountComboBox;  // 账户选择框
    private JPasswordField passwordField;       // 密码输入框
    private JButton loginBtn;                   // 登录按钮
    private JCheckBox publicChannelCheckBox;    // 公共频道复选框
    private JTextField serverHostField;
    private JTextField serverPortField;
    
    // 账户和群组映射（密码不再存储在客户端）
    private static final Map<String, String> ACCOUNT_GROUPS = new HashMap<>();
    
    static {
        ACCOUNT_GROUPS.put("feixuechat", "group_feixue");
        ACCOUNT_GROUPS.put("ash", "group_ash");
        ACCOUNT_GROUPS.put("antiash", "group_antiash");
        ACCOUNT_GROUPS.put("binglin", "group_binglin");
        ACCOUNT_GROUPS.put("feixuehome", "group_feixuehome");
        ACCOUNT_GROUPS.put("toney", "group_toney");
    }
    
    // 公共频道群组名
    private static final String PUBLIC_CHANNEL_GROUP = "group_public";
    
    // 回调接口，用于通知主类登录成功
    public interface LoginCallback {
        void onLoginSuccess(String username, String group, String password);
    }
    
    private LoginCallback callback;
    
    public twoyemian(LoginCallback callback) {
        this.callback = callback;
        initUI();
    }
    
    private void initUI() {
        setTitle("登录");
        setSize(470, 340);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // 标题面板
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("肥雪的群聊");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);
        
        // 登录面板
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 账户选择
        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("选择群组:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        accountComboBox = new JComboBox<>(new String[]{"feixuechat", "ash", "antiash", "binglin", "feixuehome", "toney"});
        loginPanel.add(accountComboBox, gbc);
        
        // 密码输入
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        loginPanel.add(new JLabel("群组密钥:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        passwordField = new JPasswordField(15);
        loginPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        loginPanel.add(new JLabel("服务器地址代码:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        serverHostField = new JTextField(ServerConfig.getDisplayHost(), 18);
        serverHostField.setToolTipText("支持数字IP或小写替代代码：a=1 ... i=9，z=0");
        loginPanel.add(serverHostField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        loginPanel.add(new JLabel("服务器端口:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        serverPortField = new JTextField(String.valueOf(ServerConfig.getPort()), 8);
        loginPanel.add(serverPortField, gbc);
        
        // 公共频道复选框
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        publicChannelCheckBox = new JCheckBox("进入公共频道（无需密码）");
        publicChannelCheckBox.addActionListener(e -> {
            boolean selected = publicChannelCheckBox.isSelected();
            accountComboBox.setEnabled(!selected);
            passwordField.setEnabled(!selected);
            if (selected) {
                accountComboBox.setSelectedIndex(0);
                passwordField.setText("");
            }
        });
        loginPanel.add(publicChannelCheckBox, gbc);
        
        // 登录按钮
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; 
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        loginBtn = new JButton("登录");
        loginPanel.add(loginBtn, gbc);
        
        add(loginPanel, BorderLayout.CENTER);
        
        // 事件监听
        loginBtn.addActionListener(new LoginListener());
        passwordField.addActionListener(new LoginListener());
        
        setVisible(true);
    }
    
    private class LoginListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean isPublicChannel = publicChannelCheckBox.isSelected();
            String selectedAccount = (String) accountComboBox.getSelectedItem();
            String password = new String(passwordField.getPassword()).trim();

            final int serverPort;
            try {
                serverPort = Integer.parseInt(serverPortField.getText().trim());
                if (serverPort < 1 || serverPort > 65535) {
                    throw new NumberFormatException();
                }
                ServerConfig.save(serverHostField.getText(), serverPort);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(twoyemian.this,
                        "服务器地址或端口无效", "连接设置错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!isPublicChannel && password.isEmpty()) {
                JOptionPane.showMessageDialog(twoyemian.this, 
                    "请输入密钥！", "登录失败", JOptionPane.ERROR_MESSAGE);
                passwordField.requestFocus();
                return;
            }
            
            // 在后台线程中验证密码
            new Thread(() -> {
                if (isPublicChannel) {
                    // 公共频道登录
                    loginToPublicChannel(selectedAccount);
                } else {
                    // 普通群组登录
                    verifyPassword(selectedAccount, password);
                }
            }).start();
        }
        
        private void verifyPassword(String username, String password) {
            try {
                // 连接到服务器并验证密码
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ServerConfig.getHost(), ServerConfig.getPort()), 8000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // 发送登录信息到服务器
                out.println("/login|" + username + "|" + password);
                
                // 等待服务器响应
                String response = in.readLine();
                
                // 关闭连接
                in.close();
                out.close();
                socket.close();
                
                // 处理服务器响应
                if (response != null && response.startsWith("/login_result|")) {
                    String result = response.substring(14); // 提取结果
                    if ("success".equals(result)) {
                        // 密码验证成功，通知主程序
                        SwingUtilities.invokeLater(() -> {
                            String group = ACCOUNT_GROUPS.get(username);
                            if (callback != null) {
                                callback.onLoginSuccess(username, group, password);
                            }
                            dispose(); // 关闭登录窗口
                        });
                    } else {
                        // 密码验证失败
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(twoyemian.this, 
                                "群组密码错误，请重新输入！", "登录失败", JOptionPane.ERROR_MESSAGE);
                            passwordField.setText("");
                            passwordField.requestFocus();
                        });
                    }
                } else {
                    // 服务器响应异常
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(twoyemian.this, 
                            "服务器响应异常，请稍后重试！", "登录失败", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                        passwordField.requestFocus();
                    });
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(twoyemian.this, 
                        "无法连接到服务器，请检查网络连接后再试！", "登录失败", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                    passwordField.requestFocus();
                });
            }
        }
        
        private void loginToPublicChannel(String username) {
            try {
                // 连接到服务器
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ServerConfig.getHost(), ServerConfig.getPort()), 8000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // 发送公共频道登录信息到服务器
                out.println("/login_public|" + username);
                
                // 等待服务器响应
                String response = in.readLine();
                
                // 关闭连接
                in.close();
                out.close();
                socket.close();
                
                // 处理服务器响应
                if (response != null && response.startsWith("/login_result|")) {
                    String result = response.substring(14); // 提取结果
                    if ("success".equals(result)) {
                        // 登录成功，通知主程序
                        SwingUtilities.invokeLater(() -> {
                            if (callback != null) {
                                callback.onLoginSuccess(username, PUBLIC_CHANNEL_GROUP, "");
                            }
                            dispose(); // 关闭登录窗口
                        });
                    } else {
                        // 登录失败
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(twoyemian.this, 
                                "公共频道登录失败: " + result, "登录失败", JOptionPane.ERROR_MESSAGE);
                            passwordField.setText("");
                            passwordField.requestFocus();
                        });
                    }
                } else {
                    // 服务器响应异常
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(twoyemian.this, 
                            "服务器响应异常，请稍后重试！", "登录失败", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                        passwordField.requestFocus();
                    });
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(twoyemian.this, 
                        "无法连接到服务器，请检查网络连接后再试！", "登录失败", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                    passwordField.requestFocus();
                });
            }
        }
    }
}
