package Config;

import InterFace.CollisionDetector;
import InterFace.Tank;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

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
            
            // 使用更明确的路径
            URL url = getClass().getResource("/Images/explotion.gif");
            System.out.println("尝试加载爆炸图片: " + url);
            
            // 如果资源路径加载失败，尝试绝对路径
            if (url == null) {
                String projectDir = System.getProperty("user.dir");
                File file = new File(projectDir + "/src/Images/explotion.gif");
                if (file.exists()) {
                    url = file.toURI().toURL();
                } else {
                    System.err.println("文件不存在: " + file.getAbsolutePath());
                }
            }
            
            if (url == null) {
                System.err.println("爆炸效果图片不存在，所有路径都已尝试");
                return;
            }
            
            // 加载GIF并存储每一帧
            ImageIcon icon = new ImageIcon(url);
            Image explosionImage = icon.getImage();
            
            // 确保图像已完全加载
            MediaTracker tracker = new MediaTracker(new JPanel());
            tracker.addImage(explosionImage, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 添加图像
            explosionFrames.add(explosionImage);

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
            
            // 创建爆炸效果,使用爆炸管理器
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            int explosionSize = Math.max(width, height) * 2; // 爆炸尺寸是坦克的2倍
            
            ExplosionManager.getInstance().createExplosion(centerX, centerY, explosionSize);
        }
    }

    // 更新爆炸效果
    protected void updateExplosion() {
        if (exploding && explosionFrames != null && !explosionFrames.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            // 爆炸持续时间1.5秒
            if (currentTime - lastFrameTime > 1500) {
                exploding = false; // 结束爆炸效果
            }
        }
    }

    // 绘制爆炸效果
    protected void drawExplosion(Graphics g) {
        if (exploding && explosionFrames != null && !explosionFrames.isEmpty()) {
            // 增大爆炸尺寸使其更明显
            Image explosionImage = explosionFrames.get(0);
            
            // 计算爆炸效果的绘制位置和尺寸
            int drawX = x - width/2; // 居中
            int drawY = y - height/2; // 居中
            int drawWidth = width * 2; // 双倍大小
            int drawHeight = height * 2; // 双倍大小
            
            // 使用Graphics2D以便添加更多视觉效果
            Graphics2D g2d = (Graphics2D) g.create();
            
            // 添加半透明效果
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            
            // 绘制爆炸图像
            g2d.drawImage(explosionImage, drawX, drawY, drawWidth, drawHeight, null);
            
            // 添加红色闪光效果
            g2d.setColor(new Color(255, 50, 50, 100));
            g2d.fillOval(drawX, drawY, drawWidth, drawHeight);
            
            g2d.dispose();

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

    public void setAlive(boolean b) {
        this.alive = b;
    }
}