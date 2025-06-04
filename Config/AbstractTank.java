package Config;

import InterFace.CollisionDetector;
import InterFace.Tank;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTank implements Tank {
    protected CollisionDetector collisionDetector;
    protected int x, y;          // 坦克位置
    protected int width, height; // 坦克尺寸(碰撞体积)
    protected int health;        // 生命值
    protected boolean alive;     // 是否存活
    protected int direction;     // 当前方向(0:上, 1:右, 2:下, 3:左)
    protected int speed = 5;     // 移动速度
    protected static final long FIRE_INTERVAL = 500; // 开火间隔

    // 添加爆炸效果相关字段
    protected List<Image> explosionFrames;
    protected boolean exploding = false;
    protected int explosionFrame = 0;
    protected long lastFrameTime = 0;
    protected static final int EXPLOSION_FRAME_DELAY = 100; // 每帧间隔100毫秒

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

        // 初始化爆炸效果图像
        loadExplosionImage();
    }

    // 加载爆炸图像
    private void loadExplosionImage() {
        try {
            explosionFrames = new ArrayList<>();
            
            // 尝试多种可能的路径
            URL url = getClass().getResource("/Images/explotion.gif");
            if (url == null) url = getClass().getResource("/Images/explosion.gif");
            if (url == null) url = getClass().getResource("Images/explotion.gif");
            if (url == null) url = getClass().getResource("Images/explosion.gif");
            
            // 尝试从文件系统加载
            if (url == null) {
                try {
                    url = new java.io.File("Images/explotion.gif").toURI().toURL();
                    if (url == null) url = new java.io.File("Images/explosion.gif").toURI().toURL();
                } catch (Exception e) {
                    System.err.println("尝试从文件系统加载失败: " + e.getMessage());
                }
            }
            
            System.out.println("最终爆炸图片路径: " + url);
            
            if (url == null) {
                System.err.println("爆炸效果图片不存在，所有路径都已尝试");
                return;
            }
            
            // 加载GIF
            ImageIcon icon = new ImageIcon(url);
            Image explosionImage = icon.getImage();
            explosionFrames.add(explosionImage.getScaledInstance(width + 30, height + 30, Image.SCALE_SMOOTH));
            System.out.println("爆炸图片加载成功，尺寸: " + (width+30) + "x" + (height+30));
        } catch (Exception e) {
            System.err.println("无法加载爆炸效果图像: " + e.getMessage());
            e.printStackTrace();
        }
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
        System.out.println("坦克受到伤害: " + damage + ", 当前生命值: " + health);
        
        if (health <= 0) {
            System.out.println("坦克被击毁，触发爆炸效果");
            alive = false;
            // 触发爆炸效果
            startExplosion();
        }
    }

    // 开始爆炸效果
    protected void startExplosion() {
        if (!exploding && explosionFrames != null && !explosionFrames.isEmpty()) {
            exploding = true;
            explosionFrame = 0;
            lastFrameTime = System.currentTimeMillis();
            
            // 创建一个定时器来确保爆炸效果会更新
            Timer explosionTimer = new Timer(100, e -> {
                if (!exploding) {
                    ((Timer)e.getSource()).stop();
                }
            });
            explosionTimer.start();
        }
    }

    // 更新爆炸效果
    protected void updateExplosion() {
        if (exploding && explosionFrames != null && !explosionFrames.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            // 爆炸持续0.8秒
            if (currentTime - lastFrameTime > 800) {
                System.out.println("爆炸效果结束");
                exploding = false; // 结束爆炸效果
            }
        }
    }

    // 绘制爆炸效果
    protected void drawExplosion(Graphics g) {
        if (exploding && explosionFrames != null && !explosionFrames.isEmpty()) {
            // 增大爆炸尺寸使其更明显
            Image explosionImage = explosionFrames.get(0);
            int drawX = x - 15; // 增大偏移
            int drawY = y - 15; // 增大偏移
            int drawWidth = width + 130; // 增大宽度
            int drawHeight = height + 130; // 增大高度
            g.drawImage(explosionImage, drawX, drawY, drawWidth, drawHeight, null);
            System.out.println("绘制爆炸效果，位置: " + drawX + "," + drawY);
        }
    }

    // 检查爆炸效果是否已结束
    public boolean explosionsFinished() {
        return !exploding;
    }

    // 在子类的draw方法中调用此方法来绘制爆炸效果
    public void draw(Graphics g) {
        // 如果坦克存活，绘制坦克
        if (isAlive()) {
            drawTank(g);
        }
        
        // 如果坦克已死亡，绘制爆炸效果
        if (!isAlive()) {
            drawExplosion(g);
        }

        // 更新爆炸状态
        updateExplosion();
    }

    // 子类应该实现此方法来绘制坦克
    protected abstract void drawTank(Graphics g);

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

    protected void setAlive(boolean b) {
        this.alive = b;
    }
}