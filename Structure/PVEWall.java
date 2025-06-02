package Structure;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class PVEWall {
    public PVEWall(int x, int y) {
    }

    public void draw(Graphics g) {
        // 绘制墙体的逻辑
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 50, 50); // 示例：绘制一个灰色的方块作为墙体
    }

    public Rectangle2D getCollisionBounds() {
        // 返回墙体的碰撞边界
        return new Rectangle2D.Double(0, 0, 50, 50); // 示例：返回一个50x50的矩形作为碰撞边界
    }
}
