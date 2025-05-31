package Structure;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Wall {
    private int x, y;              // 墙体位置
    private int width, height;     // 墙体尺寸
    private static int DEFAULT_SIZE = 10; // 默认墙体大小
    private static final Color WALL_COLOR = Color.BLACK;
    private static final Color BRICK_PATTERN_COLOR = new Color(50, 50, 50); // 深灰色，用于纹理

    // 通道最小宽度要求从100px提高到150px
    private static final int MIN_PASSAGE_WIDTH = 150;

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
        g.setColor(WALL_COLOR);
        g.fillRect(x, y, width, height);

        // 添加砖块纹理
        drawBrickPattern(g);
    }

    // 绘制砖块纹理
    private void drawBrickPattern(Graphics g) {
        g.setColor(BRICK_PATTERN_COLOR);

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

    // 静态方法：创建对称结构化的墙体布局，添加窗口大小检查
    public static List<Wall> createStructuredWalls(int gameWidth, int gameHeight) {
        List<Wall> walls = new ArrayList<>();

        // 检查游戏窗口是否足够大，不足以放置复杂布局时使用简化布局
        if (gameWidth < MIN_PASSAGE_WIDTH * 4 || gameHeight < MIN_PASSAGE_WIDTH * 4) {
            return createSimplifiedLayout(gameWidth, gameHeight);
        }

        // 计算通道宽度 (确保至少达到最小通道宽度)
        int passageWidth = Math.max(MIN_PASSAGE_WIDTH, gameWidth / 8);

        // 确保通道宽度是墙体大小的整数倍
        passageWidth = (passageWidth / DEFAULT_SIZE) * DEFAULT_SIZE;

        // 创建主要对称布局
        createSymmetricalLayout(walls, gameWidth, gameHeight, passageWidth);

        // 添加对称的战术掩体，仅当空间足够时
        if (gameWidth >= MIN_PASSAGE_WIDTH * 6 && gameHeight >= MIN_PASSAGE_WIDTH * 6) {
            createSymmetricalCovers(walls, gameWidth, gameHeight, passageWidth);
        }

        return walls;
    }

    // 为窗口过小的情况创建简化布局
    private static List<Wall> createSimplifiedLayout(int gameWidth, int gameHeight) {
        List<Wall> walls = new ArrayList<>();
        int wallSize = DEFAULT_SIZE;

        // 只在中央放置一个简单的十字形
        int centerX = gameWidth / 2;
        int centerY = gameHeight / 2;
        int crossSize = Math.min(5, Math.min(gameWidth, gameHeight) / (3 * wallSize));

        // 确保十字形不会太大
        createCrossShape(walls, centerX, centerY, crossSize, wallSize);

        return walls;
    }

    // 创建对称的主要布局
    private static void createSymmetricalLayout(List<Wall> walls, int gameWidth, int gameHeight, int passageWidth) {
        int wallSize = DEFAULT_SIZE;

        // 动态调整布局参数，确保墙体不会堆叠
        int horizontalWallLength = Math.max(0, (gameWidth - 3 * passageWidth) / 2);
        int verticalWallLength = Math.max(0, (gameHeight - 3 * passageWidth) / 2);

        // 如果计算出的墙体长度太短，就不创建
        if (horizontalWallLength < wallSize * 3 || verticalWallLength < wallSize * 3) {
            return;
        }

        // 水平墙体 - 上部
        int y1 = gameHeight / 4;
        // 左侧水平墙
        for (int x = passageWidth; x < passageWidth + horizontalWallLength; x += wallSize) {
            walls.add(new Wall(x, y1));
        }
        // 右侧水平墙
        for (int x = gameWidth - passageWidth - horizontalWallLength; x < gameWidth - passageWidth; x += wallSize) {
            walls.add(new Wall(x, y1));
        }

        // 水平墙体 - 下部
        int y2 = gameHeight * 3 / 4;
        // 左侧水平墙
        for (int x = passageWidth; x < passageWidth + horizontalWallLength; x += wallSize) {
            walls.add(new Wall(x, y2));
        }
        // 右侧水平墙
        for (int x = gameWidth - passageWidth - horizontalWallLength; x < gameWidth - passageWidth; x += wallSize) {
            walls.add(new Wall(x, y2));
        }

        // 垂直墙体 - 左侧
        int x1 = gameWidth / 4;
        // 上部垂直墙
        for (int y = passageWidth; y < passageWidth + verticalWallLength; y += wallSize) {
            walls.add(new Wall(x1, y));
        }
        // 下部垂直墙
        for (int y = gameHeight - passageWidth - verticalWallLength; y < gameHeight - passageWidth; y += wallSize) {
            walls.add(new Wall(x1, y));
        }

        // 垂直墙体 - 右侧
        int x2 = gameWidth * 3 / 4;
        // 上部垂直墙
        for (int y = passageWidth; y < passageWidth + verticalWallLength; y += wallSize) {
            walls.add(new Wall(x2, y));
        }
        // 下部垂直墙
        for (int y = gameHeight - passageWidth - verticalWallLength; y < gameHeight - passageWidth; y += wallSize) {
            walls.add(new Wall(x2, y));
        }
    }

    // 创建对称的战术掩体
    private static void createSymmetricalCovers(List<Wall> walls, int gameWidth, int gameHeight, int passageWidth) {
        int wallSize = DEFAULT_SIZE;
        int centerX = gameWidth / 2;
        int centerY = gameHeight / 2;

        // 计算可用空间
        int availableWidth = gameWidth - 2 * passageWidth;
        int availableHeight = gameHeight - 2 * passageWidth;

        // 十字形中心掩体 (确保中心点有足够空间)
        int crossSize = Math.min(availableWidth, availableHeight) / 12;
        crossSize = Math.max(3, crossSize / wallSize) * wallSize; // 确保是墙体大小的整数倍
        createCrossShape(walls, centerX, centerY, crossSize / wallSize, wallSize);

        // 四个角落的T形掩体
        int cornerOffset = passageWidth * 3 / 2;
        int tShapeSize = Math.min(4, (Math.min(gameWidth, gameHeight) / (wallSize * 10)));

        // 只有在有足够空间的情况下才创建T形掩体
        if (gameWidth > passageWidth * 3 && gameHeight > passageWidth * 3) {
            // 左上T形
            createTShape(walls, cornerOffset, cornerOffset, tShapeSize, tShapeSize, wallSize, 0);

            // 右上T形
            createTShape(walls, gameWidth - cornerOffset, cornerOffset, tShapeSize, tShapeSize, wallSize, 1);

            // 左下T形
            createTShape(walls, cornerOffset, gameHeight - cornerOffset, tShapeSize, tShapeSize, wallSize, 2);

            // 右下T形
            createTShape(walls, gameWidth - cornerOffset, gameHeight - cornerOffset, tShapeSize, tShapeSize, wallSize, 3);
        }

        // 添加对称的方块掩体，在空间足够时
        if (availableWidth > passageWidth * 2 && availableHeight > passageWidth * 2) {
            int blockOffset = passageWidth;
            int blockSize = 2; // 较小的块大小

            // 四个象限各添加一个方块掩体
            createBlockShape(walls, centerX - blockOffset - wallSize*blockSize, centerY - blockOffset - wallSize*blockSize, blockSize, blockSize, wallSize);
            createBlockShape(walls, centerX + blockOffset, centerY - blockOffset - wallSize*blockSize, blockSize, blockSize, wallSize);
            createBlockShape(walls, centerX - blockOffset - wallSize*blockSize, centerY + blockOffset, blockSize, blockSize, wallSize);
            createBlockShape(walls, centerX + blockOffset, centerY + blockOffset, blockSize, blockSize, wallSize);
        }
    }

    // 创建十字形墙体
    private static void createCrossShape(List<Wall> walls, int centerX, int centerY, int size, int wallSize) {
        // 水平部分
        for (int i = -size/2; i <= size/2; i++) {
            walls.add(new Wall(centerX + i*wallSize, centerY));
        }

        // 垂直部分
        for (int i = -size/2; i <= size/2; i++) {
            if (i != 0) { // 避免中心重复
                walls.add(new Wall(centerX, centerY + i*wallSize));
            }
        }
    }

    // 创建T形掩体 (方向: 0=上, 1=右, 2=下, 3=左)
    private static void createTShape(List<Wall> walls, int startX, int startY, int width, int height, int wallSize, int direction) {
        switch (direction) {
            case 0: // T朝上
                // 垂直线
                for (int i = 0; i < height; i++) {
                    walls.add(new Wall(startX, startY + i*wallSize));
                }
                // 水平线
                for (int i = -width/2; i <= width/2; i++) {
                    walls.add(new Wall(startX + i*wallSize, startY));
                }
                break;

            case 1: // T朝右
                // 水平线
                for (int i = -height/2; i <= height/2; i++) {
                    walls.add(new Wall(startX, startY + i*wallSize));
                }
                // 垂直线
                for (int i = 0; i < width; i++) {
                    walls.add(new Wall(startX - i*wallSize, startY));
                }
                break;

            case 2: // T朝下
                // 垂直线
                for (int i = -height; i < 0; i++) {
                    walls.add(new Wall(startX, startY + i*wallSize));
                }
                // 水平线
                for (int i = -width/2; i <= width/2; i++) {
                    walls.add(new Wall(startX + i*wallSize, startY));
                }
                break;

            case 3: // T朝左
                // 水平线
                for (int i = -height/2; i <= height/2; i++) {
                    walls.add(new Wall(startX, startY + i*wallSize));
                }
                // 垂直线
                for (int i = 0; i < width; i++) {
                    walls.add(new Wall(startX + i*wallSize, startY));
                }
                break;
        }
    }

    // 创建方形掩体
    private static void createBlockShape(List<Wall> walls, int startX, int startY, int width, int height, int wallSize) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                walls.add(new Wall(startX + x*wallSize, startY + y*wallSize));
            }
        }
    }

    // 静态方法：生成随机内部墙体，保持对称
    public static List<Wall> createRandomWalls(int gameWidth, int gameHeight, int count) {
        List<Wall> walls = new ArrayList<>();
        if (gameWidth <= DEFAULT_SIZE * 4 || gameHeight <= DEFAULT_SIZE * 4) {
            return walls;
        }

        Random random = new Random();
        int wallSize = DEFAULT_SIZE;
        int margin = wallSize * 2;
        int centerMargin = MIN_PASSAGE_WIDTH; // 中心区域保持空旷

        int availableWidth = gameWidth - margin * 2 - centerMargin;
        int availableHeight = gameHeight - margin * 2 - centerMargin;

        if (availableWidth <= 0 || availableHeight <= 0) {
            return walls;
        }

        // 减少随机墙体数量，确保不会过度拥挤
        count = Math.min(count, (availableWidth * availableHeight) / (MIN_PASSAGE_WIDTH * MIN_PASSAGE_WIDTH));

        for (int i = 0; i < count; i++) {
            try {
                int x = margin + random.nextInt(availableWidth / 2);
                int y = margin + random.nextInt(availableHeight / 2);

                // 确保x和y是wallSize的倍数
                x = (x / wallSize) * wallSize;
                y = (y / wallSize) * wallSize;

                // 避免在中心区域生成墙体
                if (Math.abs(x - gameWidth/2) < centerMargin &&
                        Math.abs(y - gameHeight/2) < centerMargin) {
                    i--;
                    continue;
                }

                // 计算对称点
                int mirrorX = gameWidth - x - wallSize;
                int mirrorY = gameHeight - y - wallSize;

                // 检查是否与现有墙体重叠或太近
                boolean tooClose = false;
                for (Wall wall : walls) {
                    if (Math.abs(wall.x - x) < MIN_PASSAGE_WIDTH/4 && Math.abs(wall.y - y) < MIN_PASSAGE_WIDTH/4) {
                        tooClose = true;
                        break;
                    }
                    if (Math.abs(wall.x - mirrorX) < MIN_PASSAGE_WIDTH/4 && Math.abs(wall.y - mirrorY) < MIN_PASSAGE_WIDTH/4) {
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose) {
                    walls.add(new Wall(x, y));
                    walls.add(new Wall(mirrorX, mirrorY)); // 添加对称的墙体
                    i++; // 额外增加计数，因为我们一次添加了两个
                } else {
                    i--;
                }
            } catch (IllegalArgumentException e) {
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