package Structure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


//开始动画
public class Start_Animation {
    //出场动画
    private static final int ANIMATION_DURATION = 1000;// 动画总时长(毫秒)
    private static final int ANIMATION_STEPS = 50;// 动画步数
    private static float currentOpacity = 0f;// 当前透明度(0-1)
    protected static Timer animationTimer;
    //图标
    protected JLabel3D logo1, logo2; // logo
    protected JPanel logoPanel;
    private Runnable animationCompleteListener;

    public void setAnimationCompleteListener(Runnable listener) {
        this.animationCompleteListener = listener;
    }

    public Start_Animation(JFrame frame) {
        initLogo(frame);
    }

    private void initLogo(JFrame frame) {
        //logo出场动画
        logoPanel = new JPanel(new GridBagLayout());
        logoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        logo1 = new JLabel3D("tank");
        logo1.setForeground(new Color(255, 215, 0)); // 金色
        logo1.setFont(new Font("Ink Free", Font.BOLD, 100));
        logo2 = new JLabel3D("War");
        logo2.setForeground(new Color(255, 0, 0));
        logo2.setFont(new Font("Mistral", Font.BOLD, 100));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        logoPanel.add(logo1, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        logoPanel.add(logo2, gbc);
        frame.add(logoPanel, BorderLayout.CENTER);
        frame.getContentPane().setBackground(Color.BLACK);
        logo1.setOpacity(0f);
        logo2.setOpacity(0f);
        // 开始淡入动画
        animationTimer = new Timer(ANIMATION_DURATION/ ANIMATION_STEPS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentOpacity += 1f/ANIMATION_STEPS;
                logo1.setOpacity(currentOpacity);
                logo2.setOpacity(currentOpacity);
                if(currentOpacity >= 1f){
                    currentOpacity = 1f;
                    animationTimer.stop();
                    // 淡入完成后，延迟0.5秒开始淡出
                    Timer delayTimer=new Timer(200,ev-> {
                        //启动淡出动画
                        animationTimer=new Timer(ANIMATION_DURATION/ANIMATION_STEPS, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                currentOpacity -= 1f/ANIMATION_STEPS;
                                logo1.setOpacity(currentOpacity);
                                logo2.setOpacity(currentOpacity);
                                if(currentOpacity <= 0f){
                                    currentOpacity = 0f;
                                    animationTimer.stop();
                                    //动画完成后移除标签
                                    frame.remove(logoPanel);
                                    frame.revalidate();
                                    frame.repaint();
                                    if (animationCompleteListener != null) {
                                        animationCompleteListener.run();
                                    }
                                }
                            }
                        });
                        animationTimer.start();
                    });
                    delayTimer.setRepeats(false);
                    delayTimer.start();
                }
            }
        });
        animationTimer.start();

    }

    protected static class JLabel3D extends JLabel {
        private float opacity = 1.0f;

        public JLabel3D(String text) {
            super(text);
        }

        public void setOpacity(float opacity) {
            this.opacity =Math.max(0f, Math.min(1f, opacity));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            g2d.setColor(Color.GRAY);
            for (int i = 1; i <= 3; i++) {
                g2d.drawString(getText(), i, i + getFont().getSize());
            }

            g2d.setColor(getForeground());
            g2d.drawString(getText(), 0, getFont().getSize());

            g2d.dispose();
        }
    }
}
