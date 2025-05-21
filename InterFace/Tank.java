package InterFace;

public interface Tank {
    // 移动方法
    void moveUp();    // 向上移动 ↑
    void moveDown();  // 向下移动 ↓
    void turnLeft();  // 左转 ←
    void turnRight(); // 右转 →

    // 射击方法
    void fire();      // 发射子弹 (空格)

    // 技能方法
    void useSkill();  // 使用技能 (X)

    // 碰撞检测相关方法
    boolean checkCollision(int newX, int newY); // 检查指定位置是否会发生碰撞
    int getX();       // 获取当前X坐标
    int getY();       // 获取当前Y坐标
    int getWidth();   // 获取坦克宽度(碰撞体积)
    int getHeight();  // 获取坦克高度(碰撞体积)

    // 坦克状态
    boolean isAlive(); // 坦克是否存活
    void takeDamage(int damage); // 受到伤害

}