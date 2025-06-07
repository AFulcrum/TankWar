package Structure;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PVEWall {
    private Rectangle bounds;
    private static final int MIN_WIDTH = 30;
    private static final int MAX_WIDTH = 120;
    private static final int MIN_HEIGHT = 30;
    private static final int MAX_HEIGHT = 120;
    private static final int MIN_GAP = 100; // 最小间隙要求
    
    // 墙体类型
    private static final int TYPE_L = 0;    // L形墙体
    private static final int TYPE_T = 1;    // T形墙体
    private static final int TYPE_Z = 2;    // Z形墙体
    private static final int TYPE_CROSS = 3; // 十字形墙体
    private static final int TYPE_BLOCK = 4; // 块形墙体
    
    // 墙体属性
    private int type;
    private List<Rectangle> segments;
    private boolean isSolid; // 是否为实心墙

    /**
     * 创建一个矩形墙体（基础形式）
     */
    public PVEWall(int x, int y, int width, int height) {
        bounds = new Rectangle(x, y, width, height);
        segments = new ArrayList<>();
        segments.add(bounds);
        type = TYPE_BLOCK;
        isSolid = true;
    }
    
    /**
     * 创建一个复杂形状的墙体
     */
    public PVEWall(int x, int y, int width, int height, int type) {
        this.type = type;
        this.segments = new ArrayList<>();
        
        // 墙体厚度
        int thickness = Math.min(30, Math.min(width, height) / 3);
        
        switch (type) {
            case TYPE_L:
                // L形墙体
                segments.add(new Rectangle(x, y, width, thickness));
                segments.add(new Rectangle(x, y, thickness, height));
                break;
                
            case TYPE_T:
                // T形墙体
                segments.add(new Rectangle(x, y, width, thickness));
                segments.add(new Rectangle(x + width/2 - thickness/2, y, thickness, height));
                break;
                
            case TYPE_Z:
                // Z形墙体
                segments.add(new Rectangle(x, y, width/2, thickness));
                segments.add(new Rectangle(x + width/4, y, thickness, height));
                segments.add(new Rectangle(x + width/2, y + height - thickness, width/2, thickness));
                break;
                
            case TYPE_CROSS:
                // 十字形墙体
                segments.add(new Rectangle(x, y + height/2 - thickness/2, width, thickness));
                segments.add(new Rectangle(x + width/2 - thickness/2, y, thickness, height));
                break;
                
            case TYPE_BLOCK:
            default:
                // 矩形墙体
                segments.add(new Rectangle(x, y, width, height));
                break;
        }
        
        // 计算包围盒
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        
        for (Rectangle segment : segments) {
            minX = Math.min(minX, segment.x);
            minY = Math.min(minY, segment.y);
            maxX = Math.max(maxX, segment.x + segment.width);
            maxY = Math.max(maxY, segment.y + segment.height);
        }
        
        bounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);
        isSolid = false;
    }

    /**
     * 获取碰撞边界
     */
    public Rectangle getCollisionBounds() {
        return bounds;
    }
    
    /**
     * 获取所有墙体段落
     */
    public List<Rectangle> getSegments() {
        return segments;
    }
    
    /**
     * 检查与另一个墙体的碰撞，包括安全间隙
     */
    public boolean collidesWith(PVEWall other) {
        // 先检查包围盒是否有足够间隙
        if (bounds.x > other.bounds.x + other.bounds.width + MIN_GAP ||
            other.bounds.x > bounds.x + bounds.width + MIN_GAP ||
            bounds.y > other.bounds.y + other.bounds.height + MIN_GAP ||
            other.bounds.y > bounds.y + bounds.height + MIN_GAP) {
            return false;  // 有足够间隙，不碰撞
        }
        
        // 没有足够间隙，认为碰撞
        return true;
    }
    
    /**
     * 检查是否与玩家或AI坦克碰撞
     */
    public boolean collidesWithTank(Rectangle tank) {
        if (tank == null) return false;
        
        // 为坦克添加一个安全距离
        Rectangle expandedTank = new Rectangle(
            tank.x - MIN_GAP/2, 
            tank.y - MIN_GAP/2, 
            tank.width + MIN_GAP, 
            tank.height + MIN_GAP
        );
        
        if (!isSolid) {
            // 对于非实心墙，检查每个段落
            for (Rectangle segment : segments) {
                if (segment.intersects(expandedTank)) {
                    return true;
                }
            }
            return false;
        } else {
            // 对于实心墙，直接检查包围盒
            return bounds.intersects(expandedTank);
        }
    }

    /**
     * 生成适合游戏区域大小的墙体集合
     */
    public static PVEWall[] generateWalls(int areaWidth, int areaHeight, Rectangle playerPos, Rectangle aiPos) {
        int gridSize = 150; // 网格大小
        int cols = Math.max(3, areaWidth / gridSize);
        int rows = Math.max(3, areaHeight / gridSize);
        
        // 估计可以放置的墙体数量
        int maxWalls = (cols * rows) / 4; // 每四个格子放一个墙体
        List<PVEWall> wallList = new ArrayList<>();
        
        // 记录已占用的区域
        boolean[][] occupied = new boolean[cols][rows];
        
        // 标记玩家和AI位置为已占用
        markTankPosition(occupied, playerPos, gridSize, cols, rows);
        markTankPosition(occupied, aiPos, gridSize, cols, rows);
        
        // 迷宫生成参数
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = cols * rows * 2;
        
        // 尝试放置墙体
        while (wallList.size() < maxWalls && attempts < maxAttempts) {
            attempts++;
            
            // 随机选择网格位置
            int gridX = random.nextInt(cols);
            int gridY = random.nextInt(rows);
            
            // 检查此位置及周围是否已占用
            if (isGridAvailable(occupied, gridX, gridY, cols, rows)) {
                // 确定墙体类型 - 随机选择不同形状增加变化
                int wallType = random.nextInt(5); // 0-4对应不同形状
                
                // 随机墙体尺寸 - 保持在合理范围内
                int width = MIN_WIDTH + random.nextInt(MAX_WIDTH - MIN_WIDTH);
                int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT);
                
                // 计算实际坐标 (居中放置在网格中)
                int x = gridX * gridSize + (gridSize - width) / 2;
                int y = gridY * gridSize + (gridSize - height) / 2;
                
                // 创建墙体
                PVEWall wall = new PVEWall(x, y, width, height, wallType);
                
                // 检查是否与已有墙体碰撞
                boolean collision = false;
                for (PVEWall existingWall : wallList) {
                    if (wall.collidesWith(existingWall)) {
                        collision = true;
                        break;
                    }
                }
                
                // 检查是否与玩家或AI碰撞
                if (!collision && !wall.collidesWithTank(playerPos) && !wall.collidesWithTank(aiPos)) {
                    wallList.add(wall);
                    
                    // 标记此位置为已占用
                    markOccupiedArea(occupied, gridX, gridY, cols, rows);
                }
            }
        }
        
        // 添加围墙
        addBoundaryWalls(wallList, areaWidth, areaHeight);
        
        return wallList.toArray(new PVEWall[0]);
    }
    
    /**
     * 标记坦克位置为已占用
     */
    private static void markTankPosition(boolean[][] occupied, Rectangle tankPos, int gridSize, int cols, int rows) {
        if (tankPos == null) return;
        
        // 计算坦克占用的网格坐标范围
        int gridX1 = Math.max(0, tankPos.x / gridSize);
        int gridY1 = Math.max(0, tankPos.y / gridSize);
        int gridX2 = Math.min(cols - 1, (tankPos.x + tankPos.width) / gridSize);
        int gridY2 = Math.min(rows - 1, (tankPos.y + tankPos.height) / gridSize);
        
        // 标记坦克及其周围的网格为已占用
        for (int gx = Math.max(0, gridX1 - 1); gx <= Math.min(cols - 1, gridX2 + 1); gx++) {
            for (int gy = Math.max(0, gridY1 - 1); gy <= Math.min(rows - 1, gridY2 + 1); gy++) {
                occupied[gx][gy] = true;
            }
        }
    }
    
    /**
     * 检查网格是否可用
     */
    private static boolean isGridAvailable(boolean[][] occupied, int x, int y, int cols, int rows) {
        // 检查是否在边界内
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            return false;
        }
        
        // 检查当前网格是否已占用
        if (occupied[x][y]) {
            return false;
        }
        
        // 检查是否靠近边缘 (为边缘留出空间)
        if (x == 0 || x == cols - 1 || y == 0 || y == rows - 1) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 标记已占用区域
     */
    private static void markOccupiedArea(boolean[][] occupied, int x, int y, int cols, int rows) {
        // 标记当前位置为已占用
        occupied[x][y] = true;
        
        // 标记周围的网格为已占用，确保墙体间距
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) {
                    occupied[nx][ny] = true;
                }
            }
        }
    }
    
    /**
     * 添加围墙 - 修复边界不覆盖整个游戏区域的问题
     */
    private static void addBoundaryWalls(List<PVEWall> walls, int width, int height) {
        int wallThickness = 20;
        
        // 确保墙体完全覆盖边界，不留缝隙
        // 上边界 - 完全覆盖顶部
        walls.add(new PVEWall(0, 0, width, wallThickness));
        
        // 下边界 - 完全覆盖底部
        walls.add(new PVEWall(0, height - wallThickness, width, wallThickness));
        
        // 左边界 - 完全覆盖左侧（包括角落）
        walls.add(new PVEWall(0, 0, wallThickness, height));
        
        // 右边界 - 完全覆盖右侧（包括角落）
        walls.add(new PVEWall(width - wallThickness, 0, wallThickness, height));
    }

    /**
     * 绘制墙体
     */
    public void draw(Graphics g) {
        // 将黑色墙体改为灰白色
        g.setColor(new Color(220, 220, 220)); // 灰白色
        
        if (isSolid) {
            // 实心墙直接填充整个矩形
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            // 添加边框使墙体更清晰
            g.setColor(new Color(180, 180, 180));
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            // 复杂形状墙体分段绘制
            for (Rectangle segment : segments) {
                g.fillRect(segment.x, segment.y, segment.width, segment.height);
                // 添加边框
                g.setColor(new Color(180, 180, 180));
                g.drawRect(segment.x, segment.y, segment.width, segment.height);
            }
        }
    }

    public boolean isSolid() {
        return isSolid;
    }
}