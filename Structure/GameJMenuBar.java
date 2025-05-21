package Structure;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;


public class GameJMenuBar {
    public GameJMenuBar(JFrame frame) {
        initJMenuBar(frame);
    }
    private void initJMenuBar(JFrame frame) {
        //初始化菜单 创建整个菜单对象
        javax.swing.JMenuBar JMenuBar = new javax.swing.JMenuBar();
        JMenuBar.setPreferredSize(new Dimension(1000,20));
        //创建菜单上的选项的对象(功能  关于)
        JMenu functionJMenu =new JMenu("功能");
        JMenu MoneyJMenu=new JMenu("变强");
        JMenu aboutJMenu =new JMenu("关于");
        //功能选项下条目
        JMenuItem replayItem =new JMenuItem("重启游戏");
        replayItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Start_Animation.animationTimer != null) {
                    Start_Animation.animationTimer.stop();
                }
                if (HomeIcon.animationTimer != null) {
                    HomeIcon.animationTimer.stop();
                }
                frame.dispose();
                GameJFrame.resetInstance();
                EventQueue.invokeLater(()->{
                    GameJFrame frame = GameJFrame.getInstance();
                    //开始动画
                    Start_Animation startAnimation = new Start_Animation(frame);
                    startAnimation.setAnimationCompleteListener(() -> {
                        // 动画完成后执行以下代码
                        frame.getContentPane().removeAll();
                        frame.getContentPane().setBackground(Color.WHITE);
                        // 创建菜单
                        new GameJMenuBar(frame);
                        // 创建HomeIcon
                        HomeIcon homeIcon = new HomeIcon(frame);
                        // 创建模式控制
                        new ModeCardLayOut(frame, homeIcon);
                        frame.revalidate();
                        frame.repaint();
                    });
                    frame.setVisible(true);
                });

            }
        });
        JMenuItem exit=new JMenuItem("关闭游戏");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        JMenuItem paypal =new JMenuItem("充钱");
        paypal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog payDialog = new JDialog(frame,"付款码",true);
                payDialog.setLayout(new BorderLayout(10, 10));
                payDialog.setSize(500, 500);
                JLabel textPay=new JLabel("付款码", SwingConstants.CENTER);
                textPay.setFont(new Font("微软雅黑", Font.BOLD, 50));
                ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/Images/12311.png")));
                Image image = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(image));
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                payDialog.add(textPay,BorderLayout.NORTH);
                payDialog.add(imageLabel, BorderLayout.CENTER);
                payDialog.setLocationRelativeTo(frame);
                payDialog.setVisible(true);
            }
        });
        JMenuItem telephone=new JMenuItem("联系方式");
        telephone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog telephone=new JDialog(frame,"联系方式",true);
                telephone.setLayout(new BorderLayout(10, 10));
                telephone.setSize(240, 240);
                ImageIcon gifIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/Videos/R-C.gif")));
                JLabel gifLabel = new JLabel(gifIcon);
                gifLabel.setHorizontalAlignment(SwingConstants.CENTER);
                telephone.add(gifLabel,BorderLayout.CENTER);
                telephone.setLocationRelativeTo(frame);
                telephone.setVisible(true);
            }
        });
        //将相应的条目添加进选项
        functionJMenu.add(replayItem);
        functionJMenu.add(exit);
        MoneyJMenu.add(paypal);
        aboutJMenu.add(telephone);
        //将选项添加到菜单
        JMenuBar.add(functionJMenu);
        JMenuBar.add(MoneyJMenu);
        JMenuBar.add(aboutJMenu);
        //给整个界面设置菜单
        frame.setJMenuBar(JMenuBar);
    }
}
