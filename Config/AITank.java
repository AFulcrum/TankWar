package Config;

import InterFace.CollisionDetector;

public class AITank extends AbstractTank {
    public AITank(int x, int y, CollisionDetector collisionDetector) {
        super(x, y, 32, 32, 1, collisionDetector); // 敌方坦克1滴血
    }

    public void updateAI() {
        // 简单的AI行为
        // 可以添加更复杂的AI逻辑
        double rand = Math.random();
        if (rand < 0.25) {
            moveUp();
        } else if (rand < 0.5) {
            moveDown();
        } else if (rand < 0.75) {
            turnLeft();
        } else {
            turnRight();
        }

        // 随机射击
        if (Math.random() < 0.02) {
            fire();
        }
    }

    @Override
    public void fire() {
        int bulletX = x + width/2;
        int bulletY = y + height/2;
        // 创建子弹并添加到游戏世界
        // Bullet bullet = new Bullet(bulletX, bulletY, direction);
        // GameWorld.addBullet(bullet);
    }

    @Override
    public void useSkill() {
        // AI坦克的特殊技能
    }

    @Override
    public int getDirection() {
        return 0;
    }
}