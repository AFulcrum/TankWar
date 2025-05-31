package Config;

import InterFace.CollisionDetector;
import Structure.Wall;

import java.awt.*;
import java.util.List;

public class SimpleCollisionDetector implements CollisionDetector {
    private Dimension gameAreaSize;
    private List<Wall> walls;

    public SimpleCollisionDetector(Dimension gameAreaSize) {
        this.gameAreaSize = gameAreaSize;
    }
    public void setGameAreaSize(Dimension size) {
        this.gameAreaSize = size;
    }
    public void setWalls(List<Wall> walls) {
        this.walls = walls;
    }

    @Override
    public boolean isColliding(int x, int y, int width, int height) {
        if (gameAreaSize == null) return false;
        // 检查边界碰撞
        if (x < 0 || y < 0 || x + width > gameAreaSize.width || y + height > gameAreaSize.height) {
            return true;
        }
        // 检查墙体碰撞
        if (walls != null) {
            Rectangle objectBounds = new Rectangle(x, y, width, height);
            for (Wall wall : walls) {
                if (objectBounds.intersects(wall.getCollisionBounds())) {
                    return true;
                }
            }
        }
        return false;
    }
}