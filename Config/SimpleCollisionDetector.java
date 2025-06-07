package Config;

import InterFace.CollisionDetector;
import Structure.PVPWall;

import java.awt.*;
import java.util.List;

public class SimpleCollisionDetector implements CollisionDetector {
    private Dimension gameAreaSize;
    private List<PVPWall> PVPWalls;

    public SimpleCollisionDetector(Dimension gameAreaSize) {
        this.gameAreaSize = gameAreaSize;
    }
    public void setGameAreaSize(Dimension size) {
        this.gameAreaSize = size;
    }
    public void setWalls(List<PVPWall> PVPWalls) {
        this.PVPWalls = PVPWalls;
    }

    @Override
    public boolean isColliding(int x, int y, int width, int height) {
        if (gameAreaSize == null) return false;
        
        // 添加安全边距，确保不会刚好卡在边界
        int safetyMargin = 2;
        
        // 检查边界碰撞 - 使用准确的边界值
        if (x < safetyMargin ||
                y < safetyMargin ||
                x + width > gameAreaSize.width - safetyMargin ||
                y + height > gameAreaSize.height - safetyMargin) {
            return true;
        }
        
        // 检查墙体碰撞
        if (PVPWalls != null) {
            Rectangle objectBounds = new Rectangle(x, y, width, height);
            for (PVPWall PVPWall : PVPWalls) {
                Rectangle wallBounds = PVPWall.getCollisionBounds();
                // 添加预检测，提前一点检测到墙体
                if (objectBounds.intersects(wallBounds)) {
                    return true;
                }
            }
        }
        return false;
    }
}