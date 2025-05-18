import Structure.GameJFrame;
import Structure.HomeIcon;
import Structure.GameJMenuBar;
import Structure.Start_Animation;
import Mode.ModeCardLayOut;

import java.awt.*;

public class play {

    public static void main(String[] args) {
        //主框架
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
    }
}