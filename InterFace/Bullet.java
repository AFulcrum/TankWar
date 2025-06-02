package InterFace;

import java.awt.*;

public interface Bullet {
    int getSpeed();
    int getDamage();//伤害
    void updatePosition();
    //获取子弹的碰撞边界
    Rectangle getCollisionBounds();
    //判断子弹是否有效
    boolean isActive();
    //标记子弹为无效状态
    void deactivate();
    // 处理反弹
    void bounce();
    // 获取当前反弹次数
    int getBounceCount();
    // 检查是否可以继续反弹
    boolean canBounce();

    void draw(Graphics g);
}