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
        // 减小网格大小，增加网格数量
        int gridSize = 120; // 从150减小到120
        int cols = Math.max(4, areaWidth / gridSize);
        int rows = Math.max(4, areaHeight / gridSize);
        
        // 增加墙体密度 - 从每四个格子一个墙体改为每三个格子一个墙体
        int maxWalls = (cols * rows) / 3; 
        List<PVEWall> wallList = new ArrayList<>();
        
        // 记录已占用的区域
        boolean[][] occupied = new boolean[cols][rows];
        
        // 标记玩家和AI位置为已占用
        markTankPosition(occupied, playerPos, gridSize, cols, rows);
        markTankPosition(occupied, aiPos, gridSize, cols, rows);
        
        // 增加尝试次数，给算法更多放置墙体的机会
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = cols * rows * 3; // 从2倍增加到3倍
        
        // 确保通道 - 创建预定义的通道图案
        createPathways(occupied, cols, rows);
        
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
                
                // 稍微减小墙体尺寸 - 保持在合理范围内
                int width = MIN_WIDTH + random.nextInt(MAX_WIDTH - MIN_WIDTH - 10);
                int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT - 10);
                
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
        
        // 添加额外的小型墙体填充空白区域
        addSmallWalls(wallList, occupied, gridSize, cols, rows, areaWidth, areaHeight, playerPos, aiPos);
        
        // 添加围墙
        addBoundaryWalls(wallList, areaWidth, areaHeight);
        
        return wallList.toArray(new PVEWall[0]);
    }
    
    /**
     * 创建预定义的通道图案，确保地图有足够的通行空间
     */
    private static void createPathways(boolean[][] occupied, int cols, int rows) {
        // 中央十字通道
        int midX = cols / 2;
        int midY = rows / 2;
        
        // 横向通道
        for (int x = 1; x < cols - 1; x++) {
            occupied[x][midY] = true;
        }
        
        // 纵向通道
        for (int y = 1; y < rows - 1; y++) {
            occupied[midX][y] = true;
        }
        
        // 随机添加一些额外通道
        Random rand = new Random();
        
        // 额外的横向通道
        int extraHorizontal = rand.nextInt(rows-4) + 2; // 避开边缘和中心
        if (extraHorizontal == midY) extraHorizontal = (extraHorizontal + 1) % rows;
        for (int x = 1; x < cols - 1; x++) {
            occupied[x][extraHorizontal] = true;
        }
        
        // 额外的纵向通道
        int extraVertical = rand.nextInt(cols-4) + 2; // 避开边缘和中心
        if (extraVertical == midX) extraVertical = (extraVertical + 1) % cols;
        for (int y = 1; y < rows - 1; y++) {
            occupied[extraVertical][y] = true;
        }
    }
    
    /**
     * 添加额外的小型墙体填充空白区域
     */
    private static void addSmallWalls(List<PVEWall> wallList, boolean[][] occupied, int gridSize,
                                 int cols, int rows, int areaWidth, int areaHeight,
                                 Rectangle playerPos, Rectangle aiPos) {
        Random random = new Random();
        int smallWallSize = 25; // 小型墙体尺寸
        int minGap = 75; // 减小小型墙体的间隙要求
        
        // 尝试放置更多小型墙体
        int attempts = 0;
        int maxSmallWalls = (cols * rows) / 2; // 可以放置更多的小型墙体
        int smallWallsAdded = 0;
        
        while (smallWallsAdded < maxSmallWalls && attempts < cols * rows * 2) {
            attempts++;
            
            // 随机选择网格内的位置
            int gridX = random.nextInt(cols);
            int gridY = random.nextInt(rows);
            
            // 跳过已标记为通道的位置
            if (occupied[gridX][gridY]) {
                continue;
            }
            
            // 在网格内随机偏移
            int offsetX = random.nextInt(gridSize - smallWallSize);
            int offsetY = random.nextInt(gridSize - smallWallSize);
            
            // 计算实际坐标
            int x = gridX * gridSize + offsetX;
            int y = gridY * gridSize + offsetY;
            
            // 创建小型墙体
            PVEWall smallWall = new PVEWall(x, y, smallWallSize, smallWallSize);
            
            // 检查是否与现有墙体碰撞 - 使用较小的间隙要求
            boolean collision = false;
            for (PVEWall existingWall : wallList) {
                // 自定义碰撞检测，使用较小的间隙
                Rectangle existingBounds = existingWall.getCollisionBounds();
                Rectangle newBounds = smallWall.getCollisionBounds();
                
                if (newBounds.x > existingBounds.x + existingBounds.width + minGap ||
                    existingBounds.x > newBounds.x + newBounds.width + minGap ||
                    newBounds.y > existingBounds.y + existingBounds.height + minGap ||
                    existingBounds.y > newBounds.y + newBounds.height + minGap) {
                    // 有足够间隙，不碰撞
                } else {
                    collision = true;
                    break;
                }
            }
            
            // 检查是否与玩家或AI碰撞
            if (!collision && !smallWall.collidesWithTank(playerPos) && !smallWall.collidesWithTank(aiPos)) {
                wallList.add(smallWall);
                smallWallsAdded++;
                
                // 标记此位置为已占用
                occupied[gridX][gridY] = true;
            }
        }
    }

    /**
     * 修改检查网格是否可用的方法 - 允许更密集的墙体放置
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
        
        // 检查是否靠近边缘 (为边缘留出空间，但稍微放宽条件)
        if (x == 0 || x == cols - 1 || y == 0 || y == rows - 1) {
            return false;
        }
        
        // 不需要检查所有相邻格子，只检查必要的位置，允许更密集放置
        // 确保至少一个相邻位置为空，提供通行空间
        boolean hasEmptyNeighbor = false;
        for (int dx = -1; dx <= 1; dx += 2) { // 只检查上下左右四个方向
            int nx = x + dx;
            if (nx >= 0 && nx < cols && !occupied[nx][y]) {
                hasEmptyNeighbor = true;
                break;
            }
        }
        
        if (!hasEmptyNeighbor) {
            for (int dy = -1; dy <= 1; dy += 2) {
                int ny = y + dy;
                if (ny >= 0 && ny < rows && !occupied[x][ny]) {
                    hasEmptyNeighbor = true;
                    break;
                }
            }
        }
        
        return hasEmptyNeighbor;
    }

    /**
     * 修改标记已占用区域方法 - 减少标记范围，允许更密集放置
     */
    private static void markOccupiedArea(boolean[][] occupied, int x, int y, int cols, int rows) {
        // 标记当前位置为已占用
        occupied[x][y] = true;
        
        // 只标记十字方向的相邻格子，而不是所有8个方向
        // 这样可以在保留通行空间的同时允许更密集的墙体
        int[][] directions = {
            {0, -1},  // 上
            {1, 0},   // 右
            {0, 1},   // 下
            {-1, 0}   // 左
        };
        
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) {
                // 设置70%的概率标记相邻格子，增加随机性
                if (Math.random() < 0.7) {
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
     * 标记坦克位置及其周围区域为已占用，防止墙体生成在坦克位置
     */
    private static void markTankPosition(boolean[][] occupied, Rectangle tankPos, int gridSize, int cols, int rows) {
        if (tankPos == null) return;
        
        // 计算坦克所在的网格范围
        int startGridX = Math.max(0, tankPos.x / gridSize);
        int startGridY = Math.max(0, tankPos.y / gridSize);
        int endGridX = Math.min(cols - 1, (tankPos.x + tankPos.width) / gridSize);
        int endGridY = Math.min(rows - 1, (tankPos.y + tankPos.height) / gridSize);
        
        // 标记坦克位置及周围一格范围为已占用
        for (int x = Math.max(0, startGridX - 1); x <= Math.min(cols - 1, endGridX + 1); x++) {
            for (int y = Math.max(0, startGridY - 1); y <= Math.min(rows - 1, endGridY + 1); y++) {
                occupied[x][y] = true;
            }
        }
    }

    /**
     * 绘制墙体
     */
    public void draw(Graphics g) {
        // 确保墙体渲染清晰可见
        // 设置墙体填充颜色
        g.setColor(new Color(230, 230, 230)); // 更亮的灰白色
        
        if (isSolid) {
            // 实心墙直接填充整个矩形
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            // 添加边框使墙体更清晰
            g.setColor(new Color(160, 160, 160)); // 更暗的边框色
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            // 复杂形状墙体分段绘制
            for (Rectangle segment : segments) {
                g.fillRect(segment.x, segment.y, segment.width, segment.height);
                // 添加边框
                g.setColor(new Color(160, 160, 160)); // 更暗的边框色
                g.drawRect(segment.x, segment.y, segment.width, segment.height);
            }
        }
    }

    public boolean isSolid() {
        return isSolid;
    }
}