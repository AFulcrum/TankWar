package Config;

import InterFace.CollisionDetector;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class PlayerTank extends AbstractTank {
    private final boolean[] keysPressed = new boolean[512];
    private static int health = 3; // 初始生命值
    private final Image[] tankImages = new Image[2];
    private int tankType;
    private double angle = 0; // 当前朝向角度，0为向上，顺时针为正
    private boolean isMoving = false;
    private long lastImageSwitchTime = 0;
    private int currentImageIndex = 0;
    private List<PlayerBullet> bullets; // 子弹列表
    private boolean spaceKeyPressed = false; //记录空格键是否按下
    private static final long FIRE_COOLDOWN = 1500; // 冷却时间1.5秒
    private long lastFireTime = 0;
    private boolean alive = true;


    public PlayerTank(int x, int y, CollisionDetector collisionDetector) {
        super(x, y, 42, 42, 3, collisionDetector);
        this.tankType = ConfigTool.getSelectedTank();
        // 初始化子弹列表
        this.bullets = new ArrayList<>();
        loadTankImage();
    }

    private void loadTankImage() {
        try {
            String[] paths = {
                    "/Images/TankImage/tank" + tankType + "/up1.png",
                    "/Images/TankImage/tank" + tankType + "/up2.png"
            };
            for (int i = 0; i < paths.length; i++) {
                java.net.URL url = getClass().getResource(paths[i]);
                if (url == null) {
                    System.err.println("图片不存在: " + paths[i]);
                    tankImages[i] = null;
                    continue;
                }
                ImageIcon icon = new ImageIcon(url);
                tankImages[i] = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            System.err.println("无法加载坦克图像: " + e.getMessage());
        }
    }

    // 处理键盘输入
    public void handleKeyPress(int keyCode) {
        keysPressed[keyCode] = true;
        if (keyCode == KeyEvent.VK_SPACE) {
            spaceKeyPressed = true;
            fire(); // 只在按下空格时尝试发射
        }
        updateMovement();
    }

    public void handleKeyRelease(int keyCode) {
        keysPressed[keyCode] = false;
        if (keyCode == KeyEvent.VK_SPACE) {
            spaceKeyPressed = false;
        }
        updateMovement();
    }

    public void updateMovement() {
        // 旋转角度,每次转动5度
        double rotateStep = Math.toRadians(5);

        if (keysPressed[KeyEvent.VK_LEFT]) {
            angle -= rotateStep;
        } else if (keysPressed[KeyEvent.VK_RIGHT]) {
            angle += rotateStep;
        }
        // 检测是否在移动
        boolean wasMoving = isMoving;
        isMoving = keysPressed[KeyEvent.VK_UP] || keysPressed[KeyEvent.VK_DOWN];

        // 如果开始移动或停止移动，重置动画计时
        if (isMoving != wasMoving) {
            lastImageSwitchTime = System.currentTimeMillis();
        }
        // 移动
        int moveSpeed = 0;
        if (keysPressed[KeyEvent.VK_UP]) {
            moveSpeed = speed;
        } else if (keysPressed[KeyEvent.VK_DOWN]) {
            moveSpeed = -speed; // 后退速度为负
        }

        if (moveSpeed != 0) {
            int newX = (int) (x + moveSpeed * Math.sin(angle));
            int newY = (int) (y - moveSpeed * Math.cos(angle));
            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }

        if (keysPressed[KeyEvent.VK_X]) {
            useSkill();
        }
    }
    public Image getCurrentImage() {
        // 如果不移动或没有图片，返回第一张图片
        if (!isMoving || tankImages[0] == null) {
            return tankImages[0];
        }

        // 动画切换逻辑 - 每200毫秒切换一次图片
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastImageSwitchTime > 200) {
            currentImageIndex = (currentImageIndex + 1) % 2;
            lastImageSwitchTime = currentTime;
        }

        return tankImages[currentImageIndex];
    }


    @Override
    public void fire() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime >= FIRE_COOLDOWN) {
            PlayerBullet bullet = new PlayerBullet(
                    getX() + getWidth()/2,
                    getY() + getHeight()/2,
                    getAngle()
            );
            bullets.add(bullet);
            lastFireTime = currentTime;
        }
    }
    public void updateBullets() {
        // 移除失效的子弹
        bullets.removeIf(bullet -> !bullet.isActive());

        // 更新剩余子弹的位置
        for (PlayerBullet bullet : bullets) {
            bullet.updatePosition();
        }
    }
    public void drawBullets(Graphics g) {
        for (PlayerBullet bullet : bullets) {
            bullet.draw(g);
        }
    }


    @Override
    public void useSkill() {
        System.out.println("使用技能");
    }

    @Override
    public int getDirection() {
        // 将角度标准化到0-2π范围
        double normalizedAngle = (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);

        // 将角度转换为方向（0:上, 1:右, 2:下, 3:左）
        if (normalizedAngle >= Math.PI/4 && normalizedAngle < 3*Math.PI/4) {
            return 1; // 右
        } else if (normalizedAngle >= 3*Math.PI/4 && normalizedAngle < 5*Math.PI/4) {
            return 2; // 下
        } else if (normalizedAngle >= 5*Math.PI/4 && normalizedAngle < 7*Math.PI/4) {
            return 3; // 左
        } else {
            return 0; // 上
        }
    }

    @Override
    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    @Override
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public void revive() { this.alive = true; health = 3; } // 重置生命值


    public static int getHealth() {
        return health;
    }


    public double getAngle() {
        return angle;
    }

    // 获取子弹列表
    public List<PlayerBullet> getBullets() {
        if (bullets == null) {
            bullets = new ArrayList<>();
        }
        return bullets;
    }

    @Override
    public Rectangle getCollisionBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void setPosition(int playerX, int playerY) {
        this.x = playerX;
        this.y = playerY;
    }

    public boolean isShooting() {
        return false;
    }
}