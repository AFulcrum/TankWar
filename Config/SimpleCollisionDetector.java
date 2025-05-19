package Config;

import InterFace.CollisionDetector;

public class SimpleCollisionDetector implements CollisionDetector {
    @Override
    public boolean isColliding(int x, int y, int width, int height) {
        // 简单的碰撞检测，后续可以添加墙体检测
        // 检查边界
        if (x < 0 || y < 0 || x + width > 800 || y + height > 600) {
            return true;
        }
        return false;
    }
}