package Config;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Wall {
    private int x, y;              // 墙体位置
    private int width, height;     // 墙体尺寸
    private static int DEFAULT_SIZE = 10; // 默认墙体大小
    private Color wallColor = new Color(0, 0, 0); // 黑色墙体

    public Wall(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // 简化的构造函数，使用默认尺寸
    public Wall(int x, int y) {
        this(x, y, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    // 获取墙体的碰撞区域
    public Rectangle getCollisionBounds() {
        return new Rectangle(x, y, width, height);
    }

    // 绘制墙体
    public void draw(Graphics g) {
        // 设置墙体颜色
        g.setColor(wallColor);
        g.fillRect(x, y, width, height);

        // 添加墙体边框
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        // 添加砖块纹理
        drawBrickPattern(g);
    }

    // 绘制砖块纹理
    private void drawBrickPattern(Graphics g) {
        g.setColor(Color.BLACK);

        // 横向砖块线
        for (int i = 0; i <= height; i += height/4) {
            g.drawLine(x, y + i, x + width, y + i);
        }

        // 竖向砖块线
        for (int i = 0; i <= width; i += width/2) {
            g.drawLine(x + i, y, x + i, y + height);
        }

        // 错开的砖块纹理
        for (int i = 0; i < height; i += height/2) {
            g.drawLine(x + width/4, y + i, x + width/4, y + i + height/4);
            g.drawLine(x + width*3/4, y + i, x + width*3/4, y + i + height/4);
        }
    }

    // 静态方法：生成边界墙体
    public static List<Wall> createBoundaryWalls(int gameWidth, int gameHeight) {
        List<Wall> walls = new ArrayList<>();
        int wallSize = DEFAULT_SIZE;

        // 上边界
        for (int x = 0; x < gameWidth; x += wallSize) {
            walls.add(new Wall(x, 0));
        }

        // 下边界
        for (int x = 0; x < gameWidth; x += wallSize) {
            walls.add(new Wall(x, gameHeight - wallSize));
        }

        // 左边界
        for (int y = wallSize; y < gameHeight - wallSize; y += wallSize) {
            walls.add(new Wall(0, y));
        }

        // 右边界
        for (int y = wallSize; y < gameHeight - wallSize; y += wallSize) {
            walls.add(new Wall(gameWidth - wallSize, y));
        }

        return walls;
    }

    // 静态方法：生成随机内部墙体
    public static List<Wall> createRandomWalls(int gameWidth, int gameHeight, int count) {
        List<Wall> walls = new ArrayList<>();
        // 如果游戏区域太小，直接返回空列表
        if (gameWidth <= DEFAULT_SIZE * 4 || gameHeight <= DEFAULT_SIZE * 4) {
            return walls;
        }
        Random random = new Random();
        int wallSize = DEFAULT_SIZE;

        // 避免墙体生成在边界和中心区域
        int margin = wallSize * 2;
        int centerMargin = wallSize * 3; // 中心区域margin更大，避免玩家和敌人初始位置被墙体阻塞

        // 确保随机范围是正数
        int availableWidth = gameWidth - margin * 2;
        int availableHeight = gameHeight - margin * 2;

        if (availableWidth <= 0 || availableHeight <= 0) {
            return walls;
        }

        for (int i = 0; i < count; i++) {
            try {
                int x = margin + random.nextInt(availableWidth);
                int y = margin + random.nextInt(availableHeight);

                // 确保x和y是wallSize的倍数
                x = (x / wallSize) * wallSize;
                y = (y / wallSize) * wallSize;

                // 避免在中心区域生成墙体
                if (Math.abs(x - gameWidth/2) < centerMargin &&
                        Math.abs(y - gameHeight/2) < centerMargin) {
                    i--;
                    continue;
                }

                // 检查是否与现有墙体重叠或太近
                boolean tooClose = false;
                for (Wall wall : walls) {
                    if (Math.abs(wall.x - x) < wallSize && Math.abs(wall.y - y) < wallSize) {
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose) {
                    walls.add(new Wall(x, y));
                } else {
                    i--;
                }
            } catch (IllegalArgumentException e) {
                // 如果发生异常，跳过这次生成
                i--;
                continue;
            }
        }

        return walls;
    }

    // getter方法
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}