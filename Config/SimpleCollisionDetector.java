package Config;

import InterFace.CollisionDetector;
import java.awt.Dimension;

public class SimpleCollisionDetector implements CollisionDetector {
    private Dimension gameAreaSize;

    public SimpleCollisionDetector(Dimension gameAreaSize) {
        this.gameAreaSize = gameAreaSize;
    }

    public void setGameAreaSize(Dimension size) {
        this.gameAreaSize = size;
    }

    @Override
    public boolean isColliding(int x, int y, int width, int height) {
        if (gameAreaSize == null) return false;
        // 检查边界碰撞
        if (x < 0 || y < 0 || x + width > gameAreaSize.width || y + height > gameAreaSize.height) {
            return true;
        }
        return false;
    }
}