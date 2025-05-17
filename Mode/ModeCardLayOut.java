package Mode;

import Base.HomeIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ModeCardLayOut {
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
        JPanel pvpPanel = createPVPPanel(cardLayout, mainPanel);
        // 5. 创建人机对战面板
        JPanel pvePanel = createPVEPanel(cardLayout, mainPanel);
        // 将所有面板添加到主面板
        mainPanel.add(menuPanel, "Menu");
        mainPanel.add(rulesPanel, "Rules");
        mainPanel.add(tankSelectionPanel, "TankSelection");
        mainPanel.add(pvpPanel, "PVP");
        mainPanel.add(pvePanel, "PVE");
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
                switch(item) {
                    case "游戏规则":
                        cardLayout.show(mainPanel, "Rules");
                        break;
                    case "坦克选择":
                        cardLayout.show(mainPanel, "TankSelection");
                        break;
                    case "坦克战争":
                        cardLayout.show(mainPanel, "PVP");
                        break;
                    case "人机对战":
                        cardLayout.show(mainPanel, "PVE");
                        break;
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
                    ← - 向左移动
                    → - 向右移动
                    X - 使用技能
                    空格 - 发射子弹
                    
                    得分规则:
                    - 击毁坦克获得分数
                    - 率先分数达到10分时游戏结束"""
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

        AtomicReference<String> selectedTank = new AtomicReference<>(null); // 存储选中的坦克
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
                //鼠标点击
                int finalI = i;
                tankPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        for (JPanel panel : tankPanels) {
                            panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                        }
                        tankPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 13));
                        selectedTank.set(tankTypes[finalI]);
                    }
                });
            }catch(IOException e){
                System.err.println(e);
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
            selectedTank.set(null);
        });

        //确定按钮
        JButton sureButton=new JButton("确定选择");
        sureButton.addActionListener(e -> {
            if(selectedTank.get()==null){
                JOptionPane.showMessageDialog(panel,"请先选择一个坦克!", "提示", JOptionPane.WARNING_MESSAGE);
            }else {
                JOptionPane.showMessageDialog(panel, "已选择: " + selectedTank.get(), "选择确认", JOptionPane.INFORMATION_MESSAGE);
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
        panel.setBackground(new Color(255, 230, 230));

        // 标题
        JLabel title = new JLabel("坦克战争", JLabel.CENTER);
        title.setFont(new Font("华文行楷", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // 游戏区域
        JPanel gamePanel = new JPanel();
        gamePanel.setBorder(BorderFactory.createTitledBorder("游戏区域"));
        gamePanel.setPreferredSize(new Dimension(400, 300));

        // 控制说明
        JTextArea controls = new JTextArea(
                "玩家1控制: 方向键移动，空格键射击\n" +
                        "玩家2控制: WASD移动，Shift键射击"
        );
        controls.setEditable(false);
        controls.setBackground(new Color(255, 230, 230));

        // 开始和返回按钮
        JButton startButton = new JButton("开始游戏");
        JButton backButton = new JButton("返回主页");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Menu"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        buttonPanel.add(startButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(backButton);

        panel.add(title, BorderLayout.NORTH);
        panel.add(gamePanel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 创建人机对战面板
    private static JPanel createPVEPanel(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 255, 230));

        JLabel title = new JLabel("人机对战", JLabel.CENTER);
        title.setFont(new Font("华文行楷", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        //关卡数
        JPanel aiTankLevel=new JPanel();
        aiTankLevel.setBackground(new Color(255, 230, 230));
//        JLabel aiLevel=new JLabel("第"+level+"关");

        // 游戏区域
        JPanel gamePanel = new JPanel();
        gamePanel.setBorder(BorderFactory.createTitledBorder("游戏区域"));
        gamePanel.setPreferredSize(new Dimension(400, 300));

        // 开始和返回按钮
        JButton startButton = new JButton("开始游戏");
        JButton backButton = new JButton("返回主页");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Menu"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(startButton);

        panel.add(title, BorderLayout.NORTH);
//        panel.add(difficultyPanel, BorderLayout.WEST);
        panel.add(gamePanel, BorderLayout.CENTER);
//        panel.add(controls, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }
}
