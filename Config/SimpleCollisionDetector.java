package Config;

import InterFace.CollisionDetector;
import Structure.PVPWall;
import Structure.PVEWall;

import java.awt.*;
import java.util.List;

public class SimpleCollisionDetector implements CollisionDetector {
    private Dimension gameAreaSize;
    private List<PVPWall> PVPWalls;
    private List<PVEWall> pveWalls; // 添加PVEWall列表

    public SimpleCollisionDetector(Dimension gameAreaSize) {
        this.gameAreaSize = gameAreaSize;
    }
    public void setGameAreaSize(Dimension size) {
        this.gameAreaSize = size;
    }
    public void setWalls(List<PVPWall> PVPWalls) {
        this.PVPWalls = PVPWalls;
    }
    public void setPVEWalls(List<PVEWall> walls) { // 添加设置PVEWall的方法
        this.pveWalls = walls;
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

        // 检查墙体碰撞 (PVPWall)
        if (PVPWalls != null) {
            Rectangle objectBounds = new Rectangle(x, y, width, height);
            for (PVPWall wall : PVPWalls) {
                Rectangle wallBounds = wall.getCollisionBounds();
                if (objectBounds.intersects(wallBounds)) {
                    return true;
                }
            }
        }
        // 检查PVEWall碰撞
        if (pveWalls != null) {
            Rectangle objectBounds = new Rectangle(x, y, width, height);
            for (PVEWall wall : pveWalls) {
                if (wall.isSolid()) {
                    // 实心墙体直接检查边界
                    if (objectBounds.intersects(wall.getCollisionBounds())) {
                        return true;
                    }
                } else {
                    // 非实心墙体检查每个段落
                    for (Rectangle segment : wall.getSegments()) {
                        if (objectBounds.intersects(segment)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // 添加一个重载方法，用于检查坦克与坦克之间的碰撞
    public boolean isColliding(int x, int y, int width, int height, Rectangle excludeBounds) {
        boolean result = isColliding(x, y, width, height);

        // 如果需要排除特定对象（如避免自己与自己碰撞）
        if (result && excludeBounds != null) {
            Rectangle newBounds = new Rectangle(x, y, width, height);
            if (newBounds.equals(excludeBounds) || newBounds.intersects(excludeBounds)) {
                return false; // 排除自身碰撞
            }
        }

        return result;
    }
}