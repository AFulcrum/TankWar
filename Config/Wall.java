package Config;

import java.awt.*;

public class Wall {
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean destructible; // 是否可被摧毁
    private int health; // 墙体健康值，仅对可摧毁墙体有效

    // 构造函数
    public Wall(int x, int y, int width, int height, boolean destructible) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.destructible = destructible;
        this.health = destructible ? 3 : Integer.MAX_VALUE; // 可摧毁墙体有3点生命值
    }

    // 绘制墙体
    public void draw(Graphics g) {
        if (destructible) {
            // 可摧毁墙体用灰色砖块纹理
            g.setColor(new Color(169, 169, 169));
            
            // 画砖块纹理
            int brickWidth = 10;
            int brickHeight = 5;
            
            for (int i = 0; i < width; i += brickWidth) {
                for (int j = 0; j < height; j += brickHeight) {
                    g.fillRect(x + i, y + j, brickWidth - 1, brickHeight - 1);
                }
            }
            
            // 根据健康值显示墙体状态
            if (health < 3) {
                g.setColor(new Color(255, 0, 0, 100));
                g.fillRect(x, y, width, height);
            }
        } else {
            // 不可摧毁墙体用深灰色实心
            g.setColor(new Color(50, 50, 50));
            g.fillRect(x, y, width, height);
        }
    }

    // 墙体被子弹击中
    public boolean hit() {
        if (destructible) {
            health--;
            return health <= 0;
        }
        return false;
    }

    // 判断是否被摧毁
    public boolean isDestroyed() {
        return destructible && health <= 0;
    }

    // getter和setter方法
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDestructible() {
        return destructible;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}
