package Structure;

import Config.ConfigTool;
import Config.PlayerTank;
import Config.SimpleCollisionDetector;
import Mode.PVEMode;
import Mode.PVPMode;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class ModeCardLayOut {
    public static int PVPModeWidth;
    public static int PVPModeHeight;
    public static int PVEModeWidth;
    public static int PVEModeHeight;
    public ModeCardLayOut(JFrame frame, HomeIcon homeIcon) {
        initModeCardLayOut(frame,homeIcon);
    }

    private void initModeCardLayOut(JFrame frame,HomeIcon homeIcon) {
        // 创建CardLayout和主面板
        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);
        // 1. 创建主菜单面板
        JPanel menuPanel = createMenuPanel(cardLayout, mainPanel,homeIcon);
        // 2. 创建游戏规则面板
        JPanel rulesPanel = createRulesPanel(cardLayout, mainPanel);
        // 3. 创建坦克选择面板
        JPanel tankSelectionPanel = createTankSelectionPanel(cardLayout, mainPanel);
        // 4. 创建坦克战争面板
        // JPanel pvpPanel = createPVPPanel(cardLayout, mainPanel);
        // 5. 创建人机对战面板
//        JPanel pvePanel = createPVEPanel(cardLayout, mainPanel);
        // 将所有面板添加到主面板
        mainPanel.add(menuPanel, "Menu");
        mainPanel.add(rulesPanel, "Rules");
        mainPanel.add(tankSelectionPanel, "TankSelection");
        mainPanel.add(new JPanel(), "PVP"); // 占位，后续动态替换
        mainPanel.add(new JPanel(), "PVE"); // 占位，后续动态替换
//        mainPanel.add(pvePanel, "PVE");
        // 设置窗口布局

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    // 创建主菜单面板
    private static JPanel createMenuPanel(CardLayout cardLayout, JPanel mainPanel, HomeIcon homeIcon) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        //主页logo
        JPanel logoPanel = (homeIcon != null) ? homeIcon.getHomeLogoPanel() : new JPanel();
        // 菜单选项
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 100, 100, 100));

        String[] menuItems = {"游戏规则", "坦克选择", "坦克战争", "人机对战"};
        for (String item : menuItems) {
            JButton button = new JButton(item);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(200, 40));
            button.setFont(new Font("华文行楷", Font.PLAIN, 20));
            button.setMargin(new Insets(10, 20, 10, 20));
            // 添加按钮事件
            button.addActionListener(e -> {
                switch (item) {
                    case "游戏规则" -> cardLayout.show(mainPanel, "Rules");
                    case "坦克选择" -> cardLayout.show(mainPanel, "TankSelection");
                    case "坦克战争" -> {
                        // 每次都创建新的PVP面板
                        JPanel newPvpPanel = createPVPPanel(cardLayout, mainPanel);
                        // 替换现有面板
                        mainPanel.remove(3); // 移除索引为3的组件（PVP面板）
                        mainPanel.add(newPvpPanel, "PVP", 3); // 在同一位置添加新面板
                        cardLayout.show(mainPanel, "PVP");
                        // 让新创建的PVPMode获得焦点并重置
                        for (Component c : newPvpPanel.getComponents()) {
                            if (c instanceof PVPMode pvpMode) {
                                SwingUtilities.invokeLater(() -> {
                                    pvpMode.resetGame(); // 确保游戏状态被完全重置
                                    pvpMode.requestFocusInWindow();
                                });
                                break;
                            }
                        }
                    }
                    case "人机对战" -> {
                        JPanel newPvePanel = createPVEPanel(cardLayout, mainPanel);
                        mainPanel.remove(mainPanel.getComponent(3)); // 移除旧的PVE面板（假设顺序不变）
                        mainPanel.add(newPvePanel, "PVE");
                        cardLayout.show(mainPanel, "PVE");
                        // 让PVEMode获得焦点
                        Component[] comps = ((JPanel)newPvePanel).getComponents();
                        for (Component c : comps) {
                            if (c instanceof PVPMode pvp) {
                                SwingUtilities.invokeLater(pvp::requestFocusInWindow);
                            }
                        }
                    }
                }
            });
            buttonPanel.add(button);
            buttonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }
        panel.add(logoPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }
    // 创建游戏规则面板
    private static JPanel createRulesPanel(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(229, 205, 250));

        // 标题
        JLabel title = new JLabel("游戏规则", JLabel.CENTER);
        title.setFont(new Font("华文行楷", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        // 内容
        JTextArea rulesText = new JTextArea(
                """
                    游戏规则:
                    ↑ - 向上移动
                    ↓ - 向下移动
                    ← - 左转
                    → - 右转
                    空格 - 发射子弹
                    
                    得分规则:
                    - 坦克战争:击毁坦克获得分数,生命值清零结束
                    - 人机对战:得3分进入下一关,AI得10分结束"""
        );
        rulesText.setFont(new Font("华文行楷", Font.PLAIN, 20));
        rulesText.setEditable(false);
        rulesText.setLineWrap(true);
        rulesText.setWrapStyleWord(true);
        rulesText.setBackground(Color.WHITE);

        // 返回按钮
        JButton backButton = new JButton("返回主页");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Menu"));
        backButton.setFont(new Font("华文行楷", Font.PLAIN, 16));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(backButton);
        buttonPanel.setBackground(new Color(231, 216, 244));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(rulesText), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 创建坦克选择面板
    private static JPanel createTankSelectionPanel(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(159, 248, 201));

        List<JPanel> tankPanels = new ArrayList<>();// 存储所有坦克面板

        JLabel title = new JLabel("坦克选择", JLabel.CENTER);
        title.setFont(new Font("华文行楷", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel contentPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        contentPanel.setBackground(new Color(255, 255, 255));
        String[] tankTypes = {"红坦克", "蓝坦克", "绿坦克", "黄坦克"};
        for (int i = 0; i < tankTypes.length; i++) {
            JPanel tankPanel = new JPanel(new BorderLayout());
            tankPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            tankPanel.setBackground(Color.WHITE);
            tankPanels.add(tankPanel);// 添加到所有坦克的列表

            JLabel label = new JLabel(tankTypes[i], JLabel.CENTER);
            label.setFont(new Font("华文行楷", Font.BOLD, 25));

            //添加坦克图片
            String tankImagePath="/Images/TankImage/tank"+(i+1)+"/up1.png";
            try{
                URL tankImageUrl=ModeCardLayOut.class.getResource(tankImagePath);
                if(tankImageUrl==null){
                    throw new IOException("图片不存在: " + tankImagePath);
                }
                ImageIcon tankImageIcon = new ImageIcon(tankImageUrl);
                Image tankImage = tankImageIcon.getImage().getScaledInstance(150, 150, Image.SCALE_FAST);
                JLabel imageLabel = new JLabel(new ImageIcon(tankImage));
                tankPanel.add(label, BorderLayout.NORTH);
                tankPanel.add(imageLabel, BorderLayout.CENTER);
                //鼠标点击,当没有选择时才能点击有效
                int finalI = i;
                tankPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // 只有在未选择坦克时才允许选择
                        if(!ConfigTool.isTankSelected()) {
                            for (JPanel panel : tankPanels) {
                                panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                            }
                            tankPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 10));
                            ConfigTool.setTankSelection(tankTypes[finalI]);
                        }
                    }
                });

            }catch(IOException e){
//                System.err.println(e);
                JLabel placeholder = new JLabel("图片加载失败");
                placeholder.setForeground(Color.RED);
                tankPanel.add(placeholder, BorderLayout.CENTER);
            }
            contentPanel.add(tankPanel);

        }

        //重新选择
        JButton againButton=new JButton("重新选择");
        againButton.addActionListener(e -> {
            for (JPanel newpanel :tankPanels){
                newpanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            }
            ConfigTool.resetTankSelection();
        });

        //确定按钮
        JButton sureButton=new JButton("确定选择");
        sureButton.addActionListener(e -> {
            if(!ConfigTool.isTankSelected()){
                JOptionPane.showMessageDialog(panel,"请先选择一个坦克!", "提示", JOptionPane.WARNING_MESSAGE);
            }else {
                JOptionPane.showMessageDialog(panel, "已选择: " + ConfigTool.getSelectedTank(), "选择确认", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "Menu");
            }
        });

        // 返回按钮
        JButton backButton = new JButton("返回主页");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Menu"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(againButton);
        buttonPanel.add(sureButton);
        buttonPanel.setBackground(new Color(175, 248, 209));

        panel.add(title, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 创建坦克战争面板
    private static JPanel createPVPPanel(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(217, 248, 240));
        // 设置最小尺寸，确保有足够的空间生成墙体
        panel.setPreferredSize(new Dimension(800, 600));

        // 标题
        JLabel title = new JLabel("坦克战争", JLabel.CENTER);
        title.setFont(new Font("华文行楷", Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        //击败数和生命值面板
        JPanel beatPanel = new JPanel(new GridLayout(2, 1, 0, 10));// 2行1列，垂直间距10
        beatPanel.setPreferredSize(new Dimension(100, Integer.MAX_VALUE));
        beatPanel.setBackground(new Color(199, 248, 235));
        JLabel beatNumLabel = new JLabel("<html><div style='text-align: center;'>击<br>败<br>数<br>"+ ConfigTool.getBeatNum() + "</html>");
        beatNumLabel.setFont(new Font("华文行楷", Font.BOLD, 30));
        beatNumLabel.setForeground(Color.RED);
        beatNumLabel.setHorizontalAlignment(JLabel.CENTER);
        beatNumLabel.setVerticalAlignment(JLabel.CENTER);
        JLabel healthLabel=new JLabel("<html><div style='text-align: center;'>生<br>命<br>值<br>"+ PlayerTank.getHealth() + "</html>");
        healthLabel.setFont(new Font("华文行楷", Font.BOLD, 30));
        healthLabel.setForeground(Color.RED);
        healthLabel.setHorizontalAlignment(JLabel.CENTER);
        healthLabel.setVerticalAlignment(JLabel.CENTER);
        healthLabel.setBorder(BorderFactory.createEmptyBorder(100, 0, -70, 0));
        beatNumLabel.setBorder(BorderFactory.createEmptyBorder(-100, 0, 100, 0));
        beatPanel.add(healthLabel);
        beatPanel.add(beatNumLabel);

        // 游戏区域
        PVPMode pvpMode = new PVPMode(
                new SimpleCollisionDetector(new Dimension(800, 600)),
                beatNumLabel,
                healthLabel
        );
        //显示时请求焦点
        SwingUtilities.invokeLater(() -> {
            pvpMode.requestFocusInWindow();
            // 初始化碰撞检测区域
            if (pvpMode.getDetector() instanceof SimpleCollisionDetector) {
                ((SimpleCollisionDetector)pvpMode.getDetector())
                        .setGameAreaSize(pvpMode.getSize());
            }
        });
        pvpMode.setBorder(BorderFactory.createTitledBorder(" "));
        pvpMode.setBackground(Color.BLACK); // 修改背景色为黑色
        // 添加组件监听器来获取实际尺寸
        pvpMode.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                PVPModeWidth = pvpMode.getWidth();
                PVPModeHeight = pvpMode.getHeight();
                // 更新碰撞检测区域
                if (pvpMode.getDetector() instanceof SimpleCollisionDetector) {
                    ((SimpleCollisionDetector)pvpMode.getDetector())
                            .setGameAreaSize(new Dimension(PVPModeWidth, PVPModeHeight));
                }
            }
        });

        // 控制说明
        JTextArea controls = new JTextArea(
                """
                        \s\s\s\s↑ - 向上移动
                        \s\s\s\s↓ - 向下移动
                        \s\s\s\s\s← - 左转
                        \s\s\s\s\s→ - 右转
                         空格 - 发射子弹\s\s
                         击毁坦克获得分数
                         生命值清零结束
                         敌方坦克血量为1"""
        );
        controls.setEditable(false);
        controls.setBackground(new Color(175, 244, 227));
        controls.setPreferredSize(new Dimension(150, Integer.MAX_VALUE));

        // 开始,暂停和返回按钮
        JButton startButton = new JButton("开始游戏");
        startButton.addActionListener(e -> {
            pvpMode.startGame();
            pvpMode.requestFocusInWindow(); // 再次确保获得焦点
        });
        JButton pauseButton = new JButton("暂停游戏");
        pauseButton.addActionListener(e -> pvpMode.stopGame());
        JButton backButton = new JButton("返回主页");
        backButton.addActionListener(e -> {
            pvpMode.endGame();
            cardLayout.show(mainPanel, "Menu");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(pauseButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(startButton);

        panel.add(title, BorderLayout.NORTH);
        panel.add(beatPanel, BorderLayout.WEST);
        panel.add(pvpMode, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 创建人机对战面板
    private static JPanel createPVEPanel(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 255, 230));
        panel.setPreferredSize(new Dimension(800, 600));

        // 标题
        JLabel title = new JLabel("人机对战", JLabel.CENTER);
        title.setFont(new Font("华文行楷", Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // 关卡和分数显示面板
        JPanel scorePanel = new JPanel(new GridLayout(2, 1, 0, 10));
        scorePanel.setPreferredSize(new Dimension(100, Integer.MAX_VALUE));
        scorePanel.setBackground(new Color(212, 246, 212));

        JLabel levelLabel = new JLabel("<html><div style='text-align: center;'>第<br>"
                + ConfigTool.getLevel() + "<br>关</div></html>");
        JLabel scoreLabel = new JLabel("<html><div style='text-align: center;'>我方<br>"
                + ConfigTool.getOurScore() + ":" + ConfigTool.getEnemyScore()
                + "<br>敌方</div></html>");

        levelLabel.setFont(new Font("华文行楷", Font.BOLD, 30));
        scoreLabel.setFont(new Font("华文行楷", Font.BOLD, 30));
        levelLabel.setForeground(Color.RED);
        scoreLabel.setForeground(Color.RED);
        levelLabel.setHorizontalAlignment(JLabel.CENTER);
        scoreLabel.setHorizontalAlignment(JLabel.CENTER);

        scorePanel.add(levelLabel);
        scorePanel.add(scoreLabel);

        // 创建PVEMode
        PVEMode pveMode = new PVEMode(
                new SimpleCollisionDetector(new Dimension(800, 600)),
                levelLabel,
                scoreLabel
        );

        // 设置焦点
        SwingUtilities.invokeLater(() -> {
            pveMode.requestFocusInWindow();
            if (pveMode.getDetector() instanceof SimpleCollisionDetector) {
                ((SimpleCollisionDetector)pveMode.getDetector())
                        .setGameAreaSize(pveMode.getSize());
            }
        });

        // 控制说明
        JTextArea controls = new JTextArea(
                """
                        \s\s\s\s↑ - 向上移动
                        \s\s\s\s↓ - 向下移动
                        \s\s\s\s\s← - 左转
                        \s\s\s\s\s→ - 右转
                         空格 - 发射子弹\s\s
                         得3分进入下一关
                         AI得10分结束
                         敌方坦克血量为1"""
        );
        controls.setEditable(false);
        controls.setBackground(new Color(217, 250, 190));
        controls.setPreferredSize(new Dimension(150, Integer.MAX_VALUE));

        // 控制按钮
        JButton startButton = new JButton("开始游戏");
        startButton.addActionListener(e -> {
            pveMode.startGame();
            pveMode.requestFocusInWindow();
        });

        JButton pauseButton = new JButton("暂停游戏");
        pauseButton.addActionListener(e -> pveMode.stopGame());

        JButton backButton = new JButton("返回主页");
        backButton.addActionListener(e -> {
            pveMode.endGame();
            cardLayout.show(mainPanel, "Menu");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(pauseButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(startButton);

        // 组装面板
        panel.add(title, BorderLayout.NORTH);
        panel.add(scorePanel, BorderLayout.WEST);
        panel.add(pveMode, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        pveMode.setBackground(Color.BLACK); // 修改背景色为黑色

        return panel;
    }
}
