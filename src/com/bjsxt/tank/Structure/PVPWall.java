package src.com.bjsxt.tank.Structure;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PVPWall {
    private int x, y;              // 墙体位置
    private int width, height;     // 墙体尺寸
    private static int DEFAULT_SIZE = 10; // 默认墙体大小
    // 修改墙体颜色
    private static final Color WALL_COLOR = new Color(220, 220, 220); // 灰白色
    private static final Color BRICK_PATTERN_COLOR = new Color(180, 180, 180); // 浅灰色，用于纹理

    // 通道最小宽度150px
    private static final int MIN_PASSAGE_WIDTH = 150;

    public PVPWall(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // 简化的构造函数，使用默认尺寸
    public PVPWall(int x, int y) {
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
    public static List<PVPWall> createBoundaryWalls(int gameWidth, int gameHeight) {
        List<PVPWall> PVPWalls = new ArrayList<>();
        int wallSize = DEFAULT_SIZE;

        // 确保边界位置准确
        int bottomY = gameHeight - wallSize - 1; // 减1确保边界内

        // 上边界
        for (int x = 0; x < gameWidth; x += wallSize) {
            PVPWalls.add(new PVPWall(x, 1));
        }

        // 下边界 - 修正位置
        for (int x = 0; x < gameWidth; x += wallSize) {
            PVPWalls.add(new PVPWall(x, bottomY-1));
        }

        // 左边界
        for (int y = wallSize; y < bottomY; y += wallSize) {
            PVPWalls.add(new PVPWall(1, y));
        }

        // 右边界
        for (int y = wallSize; y < bottomY; y += wallSize) {
            PVPWalls.add(new PVPWall(gameWidth - wallSize-1, y));
        }

        return PVPWalls;
    }

    // 静态方法：创建对称结构化的墙体布局，添加窗口大小检查
    public static List<PVPWall> createStructuredWalls(int gameWidth, int gameHeight) {
        List<PVPWall> PVPWalls = new ArrayList<>();

        // 检查游戏窗口是否足够大，不足以放置复杂布局时使用简化布局
        if (gameWidth < MIN_PASSAGE_WIDTH * 4 || gameHeight < MIN_PASSAGE_WIDTH * 4) {
            return createSimplifiedLayout(gameWidth, gameHeight);
        }

        // 计算通道宽度 (确保至少达到最小通道宽度)
        int passageWidth = Math.max(MIN_PASSAGE_WIDTH, gameWidth / 8);

        // 确保通道宽度是墙体大小的整数倍
        passageWidth = (passageWidth / DEFAULT_SIZE) * DEFAULT_SIZE;

        // 创建主要对称布局
        createSymmetricalLayout(PVPWalls, gameWidth, gameHeight, passageWidth);

        // 添加对称的战术掩体，仅当空间足够时
        if (gameWidth >= MIN_PASSAGE_WIDTH * 6 && gameHeight >= MIN_PASSAGE_WIDTH * 6) {
            createSymmetricalCovers(PVPWalls, gameWidth, gameHeight, passageWidth);
        }

        return PVPWalls;
    }

    // 为窗口过小的情况创建简化布局
    private static List<PVPWall> createSimplifiedLayout(int gameWidth, int gameHeight) {
        List<PVPWall> PVPWalls = new ArrayList<>();
        int wallSize = DEFAULT_SIZE;

        // 只在中央放置一个简单的十字形
        int centerX = gameWidth / 2;
        int centerY = gameHeight / 2;
        int crossSize = Math.min(5, Math.min(gameWidth, gameHeight) / (3 * wallSize));

        // 确保十字形不会太大
        createCrossShape(PVPWalls, centerX, centerY, crossSize, wallSize);

        return PVPWalls;
    }

    // 创建对称的主要布局
    private static void createSymmetricalLayout(List<PVPWall> PVPWalls, int gameWidth, int gameHeight, int passageWidth) {
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
            PVPWalls.add(new PVPWall(x, y1));
        }
        // 右侧水平墙
        for (int x = gameWidth - passageWidth - horizontalWallLength; x < gameWidth - passageWidth; x += wallSize) {
            PVPWalls.add(new PVPWall(x, y1));
        }

        // 水平墙体 - 下部
        int y2 = gameHeight * 3 / 4;
        // 左侧水平墙
        for (int x = passageWidth; x < passageWidth + horizontalWallLength; x += wallSize) {
            PVPWalls.add(new PVPWall(x, y2));
        }
        // 右侧水平墙
        for (int x = gameWidth - passageWidth - horizontalWallLength; x < gameWidth - passageWidth; x += wallSize) {
            PVPWalls.add(new PVPWall(x, y2));
        }

        // 垂直墙体 - 左侧
        int x1 = gameWidth / 4;
        // 上部垂直墙
        for (int y = passageWidth; y < passageWidth + verticalWallLength; y += wallSize) {
            PVPWalls.add(new PVPWall(x1, y));
        }
        // 下部垂直墙
        for (int y = gameHeight - passageWidth - verticalWallLength; y < gameHeight - passageWidth; y += wallSize) {
            PVPWalls.add(new PVPWall(x1, y));
        }

        // 垂直墙体 - 右侧
        int x2 = gameWidth * 3 / 4;
        // 上部垂直墙
        for (int y = passageWidth; y < passageWidth + verticalWallLength; y += wallSize) {
            PVPWalls.add(new PVPWall(x2, y));
        }
        // 下部垂直墙
        for (int y = gameHeight - passageWidth - verticalWallLength; y < gameHeight - passageWidth; y += wallSize) {
            PVPWalls.add(new PVPWall(x2, y));
        }
    }

    // 创建对称的战术掩体
    private static void createSymmetricalCovers(List<PVPWall> PVPWalls, int gameWidth, int gameHeight, int passageWidth) {
        int wallSize = DEFAULT_SIZE;
        int centerX = gameWidth / 2;
        int centerY = gameHeight / 2;

        // 计算可用空间
        int availableWidth = gameWidth - 2 * passageWidth;
        int availableHeight = gameHeight - 2 * passageWidth;

        // 十字形中心掩体 (确保中心点有足够空间)
        int crossSize = Math.min(availableWidth, availableHeight) / 12;
        crossSize = Math.max(3, crossSize / wallSize) * wallSize; // 确保是墙体大小的整数倍
        createCrossShape(PVPWalls, centerX, centerY, crossSize / wallSize, wallSize);

        // 四个角落的T形掩体
        int cornerOffset = passageWidth * 3 / 2;
        int tShapeSize = Math.min(4, (Math.min(gameWidth, gameHeight) / (wallSize * 10)));

        // 只有在有足够空间的情况下才创建T形掩体
        if (gameWidth > passageWidth * 3 && gameHeight > passageWidth * 3) {
            // 左上T形
            createTShape(PVPWalls, cornerOffset, cornerOffset, tShapeSize, tShapeSize, wallSize, 0);

            // 右上T形
            createTShape(PVPWalls, gameWidth - cornerOffset, cornerOffset, tShapeSize, tShapeSize, wallSize, 1);

            // 左下T形
            createTShape(PVPWalls, cornerOffset, gameHeight - cornerOffset, tShapeSize, tShapeSize, wallSize, 2);

            // 右下T形
            createTShape(PVPWalls, gameWidth - cornerOffset, gameHeight - cornerOffset, tShapeSize, tShapeSize, wallSize, 3);
        }

        // 添加对称的方块掩体，在空间足够时
        if (availableWidth > passageWidth * 2 && availableHeight > passageWidth * 2) {
            int blockOffset = passageWidth;
            int blockSize = 2; // 较小的块大小

            // 四个象限各添加一个方块掩体
            createBlockShape(PVPWalls, centerX - blockOffset - wallSize*blockSize, centerY - blockOffset - wallSize*blockSize, blockSize, blockSize, wallSize);
            createBlockShape(PVPWalls, centerX + blockOffset, centerY - blockOffset - wallSize*blockSize, blockSize, blockSize, wallSize);
            createBlockShape(PVPWalls, centerX - blockOffset - wallSize*blockSize, centerY + blockOffset, blockSize, blockSize, wallSize);
            createBlockShape(PVPWalls, centerX + blockOffset, centerY + blockOffset, blockSize, blockSize, wallSize);
        }
    }

    // 创建十字形墙体
    private static void createCrossShape(List<PVPWall> PVPWalls, int centerX, int centerY, int size, int wallSize) {
        // 水平部分
        for (int i = -size/2; i <= size/2; i++) {
            PVPWalls.add(new PVPWall(centerX + i*wallSize, centerY));
        }

        // 垂直部分
        for (int i = -size/2; i <= size/2; i++) {
            if (i != 0) { // 避免中心重复
                PVPWalls.add(new PVPWall(centerX, centerY + i*wallSize));
            }
        }
    }

    // 创建T形掩体 (方向: 0=上, 1=右, 2=下, 3=左)
    private static void createTShape(List<PVPWall> PVPWalls, int startX, int startY, int width, int height, int wallSize, int direction) {
        switch (direction) {
            case 0: // T朝上
                // 垂直线
                for (int i = 0; i < height; i++) {
                    PVPWalls.add(new PVPWall(startX, startY + i*wallSize));
                }
                // 水平线
                for (int i = -width/2; i <= width/2; i++) {
                    PVPWalls.add(new PVPWall(startX + i*wallSize, startY));
                }
                break;

            case 1: // T朝右
                // 水平线
                for (int i = -height/2; i <= height/2; i++) {
                    PVPWalls.add(new PVPWall(startX, startY + i*wallSize));
                }
                // 垂直线
                for (int i = 0; i < width; i++) {
                    PVPWalls.add(new PVPWall(startX - i*wallSize, startY));
                }
                break;

            case 2: // T朝下
                // 垂直线
                for (int i = -height; i < 0; i++) {
                    PVPWalls.add(new PVPWall(startX, startY + i*wallSize));
                }
                // 水平线
                for (int i = -width/2; i <= width/2; i++) {
                    PVPWalls.add(new PVPWall(startX + i*wallSize, startY));
                }
                break;

            case 3: // T朝左
                // 水平线
                for (int i = -height/2; i <= height/2; i++) {
                    PVPWalls.add(new PVPWall(startX, startY + i*wallSize));
                }
                // 垂直线
                for (int i = 0; i < width; i++) {
                    PVPWalls.add(new PVPWall(startX + i*wallSize, startY));
                }
                break;
        }
    }

    // 创建方形掩体
    private static void createBlockShape(List<PVPWall> PVPWalls, int startX, int startY, int width, int height, int wallSize) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                PVPWalls.add(new PVPWall(startX + x*wallSize, startY + y*wallSize));
            }
        }
    }

    // 静态方法：生成随机内部墙体，保持对称
    public static List<PVPWall> createRandomWalls(int gameWidth, int gameHeight, int count) {
        List<PVPWall> PVPWalls = new ArrayList<>();
        if (gameWidth <= DEFAULT_SIZE * 4 || gameHeight <= DEFAULT_SIZE * 4) {
            return PVPWalls;
        }

        Random random = new Random();
        int wallSize = DEFAULT_SIZE;
        int margin = wallSize * 2;
        int centerMargin = MIN_PASSAGE_WIDTH; // 中心区域保持空旷

        int availableWidth = gameWidth - margin * 2 - centerMargin;
        int availableHeight = gameHeight - margin * 2 - centerMargin;

        if (availableWidth <= 0 || availableHeight <= 0) {
            return PVPWalls;
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
                for (PVPWall PVPWall : PVPWalls) {
                    if (Math.abs(PVPWall.x - x) < MIN_PASSAGE_WIDTH/4 && Math.abs(PVPWall.y - y) < MIN_PASSAGE_WIDTH/4) {
                        tooClose = true;
                        break;
                    }
                    if (Math.abs(PVPWall.x - mirrorX) < MIN_PASSAGE_WIDTH/4 && Math.abs(PVPWall.y - mirrorY) < MIN_PASSAGE_WIDTH/4) {
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose) {
                    PVPWalls.add(new PVPWall(x, y));
                    PVPWalls.add(new PVPWall(mirrorX, mirrorY)); // 添加对称的墙体
                    i++; // 额外增加计数，因为我们一次添加了两个
                } else {
                    i--;
                }
            } catch (IllegalArgumentException e) {
                i--;
                continue;
            }
        }

        return PVPWalls;
    }

    // getter方法
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}