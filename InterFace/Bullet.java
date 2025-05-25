package InterFace;

import java.awt.Rectangle;

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
}