package Config;

import InterFace.CollisionDetector;
import InterFace.Tank;

import java.awt.*;

public abstract class AbstractTank implements Tank {
    protected CollisionDetector collisionDetector;
    protected int x, y;          // 坦克位置
    protected int width, height; // 坦克尺寸(碰撞体积)
    protected int health;        // 生命值
    protected boolean alive;     // 是否存活
    protected int direction;     // 当前方向(0:上, 1:右, 2:下, 3:左)
    protected int speed = 5;     // 移动速度
    protected static final long FIRE_INTERVAL = 500; // 开火间隔

    public AbstractTank(int x, int y,
                        int width, int height,
                        int health,CollisionDetector collisionDetector) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.health = health;
        this.alive = true;
        this.direction = 0; // 默认朝上
        this.collisionDetector = collisionDetector;
    }

    @Override
    public void moveUp() {
        if (!isColliding(x, y - speed)) {
            y -= speed;
        }
    }

    @Override
    public void moveDown() {
        if (!isColliding(x, y + speed)) {
            y += speed;
        }
    }

    @Override
    public void turnLeft() {
        direction = (direction - 1 + 4) % 4; // 确保方向在0-3范围内
    }

    @Override
    public void turnRight() {
        direction = (direction + 1) % 4; // 确保方向在0-3范围内
    }

    @Override
    public abstract void fire(); // 射击方式

    public abstract void fire(PlayerTank player);

    @Override
    public abstract void useSkill(); // 技能

    protected boolean isColliding(int newX, int newY) {
        return collisionDetector.isColliding(newX, newY, width, height);
    }

    @Override
    public boolean checkCollision(int newX, int newY) {
        // 这里应该实现与游戏地图和其他物体的碰撞检测
        // 返回true表示可以移动，false表示会发生碰撞
        // 这是一个基本实现，实际游戏中需要更复杂的碰撞检测
        return !collisionDetector.isColliding(newX, newY, width, height);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            alive = false;
        }
    }

    public abstract int getDirection();

    public abstract Rectangle getCollisionBounds();

    public double getAngle() {
        return 0;
    }

    public Image getCurrentImage() {
        return null;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void revive();

    protected boolean isColliding(int newX, int newY, int width, int height) {
        // 检查是否与其他物体发生碰撞
        return collisionDetector.isColliding(newX, newY, width, height);
    }
}