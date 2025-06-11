package src.Structure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static src.Structure.Start_Animation.JLabel3D;
public class HomeIcon {
    //动画
    private static final int ANIMATION_DURATION = 1000;// 动画总时长(毫秒)
    private static final int ANIMATION_STEPS = 50;// 动画步数
    private static float currentOpacity = 0f;// 当前透明度(0-1)
    protected static Timer animationTimer;
    //图标
    protected JLabel3D homeLogo1, homeLogo2; // logo
    protected JPanel  homeLogoPanel;
    public JPanel getHomeLogoPanel() {
        return homeLogoPanel;
    }
    public HomeIcon(JFrame frame) {
        initHomeIcon(frame);
    }

    private void initHomeIcon(JFrame frame) {
        // 创建主界面Logo
        homeLogoPanel = new JPanel();
        homeLogoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        homeLogoPanel.setOpaque(false);
        // 创建Logo标签
        homeLogo1 = new JLabel3D("tank");
        homeLogo1.setForeground(new Color(255, 215, 0));
        homeLogo1.setFont(new Font("Ink Free", Font.BOLD, 150));

        homeLogo2 = new JLabel3D("War");
        homeLogo2.setForeground(new Color(255, 0, 0));
        homeLogo2.setFont(new Font("Mistral", Font.BOLD, 150));
        // 添加到面板
        homeLogoPanel.add(homeLogo1);
        homeLogoPanel.add(homeLogo2);
        frame.add(homeLogoPanel, BorderLayout.NORTH);
        label3DEnter(homeLogo1, homeLogo2, 0, 0);
    }

    //标签淡出
    public void label3DEnter(JLabel3D myLabel1, JLabel3D myLabel2, int t, int i) {
        myLabel1.setOpacity(0f);
        myLabel2.setOpacity(0f);
        //logo出场动画淡入
        Timer delayTimer = new Timer(t, e -> {
            animationTimer = new Timer(  ANIMATION_DURATION/ ANIMATION_STEPS, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    currentOpacity += 1f/ANIMATION_STEPS;
                    if (currentOpacity >= 1f) {
                        currentOpacity = 1f;
                        animationTimer.stop();
                    }
                    myLabel1.setOpacity(currentOpacity);
                    myLabel2.setOpacity(currentOpacity);
                }
            });
            animationTimer.start();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
}
