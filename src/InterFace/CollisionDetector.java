package src.InterFace;

//碰撞检测器
public interface CollisionDetector {
    boolean isColliding(int x, int y, int width, int height);
}