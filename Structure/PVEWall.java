package Structure;

import java.awt.*;

public class PVEWall {
    public PVEWall(int x, int y) {
    }

    public void draw(Graphics g) {
        // 绘制墙体的逻辑
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 50, 50); // 示例：绘制一个灰色的方块作为墙体
    }
}
