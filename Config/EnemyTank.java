package Config;

import InterFace.CollisionDetector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static Structure.ModeCardLayOut.PVPModeHeight;
import static Structure.ModeCardLayOut.PVPModeWidth;

public class EnemyTank extends AbstractTank {
    private boolean alive = true;
    private double angle = 0; // 当前朝向角度，0为向上，顺时针为正
    private final String tankPath = "/Images/TankImage/EnemyTank/tankU.gif";
    private Image tankImage;
    private final Random random = new Random();
    private double moveSpeed = 0.5; // 移动速度
    private double rotateStep = Math.toRadians(3); // 旋转步长，每次转动3度
    private int moveDuration = 0; // 移动持续帧数
    private int rotateDuration = 0; // 旋转持续帧数
//    private List<EnemyBullet> bullets = new ArrayList<EnemyBullet>();
    private List<EnemyBullet> bullets;
    private long lastFireTime = 0;
    private static final int FIRE_CHANCE = 66; // 每帧有1/66的几率射击
    private static final int FIRE_INTERVAL = 1000; // 射击间隔1秒
    private static final int BULLET_LIFETIME = 20000; // 子弹最大存活时间20秒


    public EnemyTank(int x, int y, CollisionDetector collisionDetector) {
        super(x, y, 66, 66, 1, collisionDetector); // 敌方坦克1滴血
        loadTankImage();
        // 初始化子弹列表
        this.bullets = new ArrayList<>();
    }

    private void loadTankImage() {
        try {
            java.net.URL url = getClass().getResource(tankPath);
            if (url == null) {
                System.err.println("图片不存在: " + tankPath);
                return;
            }
            ImageIcon icon = new ImageIcon(url);
            tankImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.err.println("无法加载坦克图像: " + e.getMessage());
        }
    }

    public void updateMovement() {
        // 随机决定是否开始新的移动或旋转
        if (moveDuration <= 0) {
            moveDuration = random.nextInt(100) + 50; // 随机移动50 - 150帧
            moveSpeed = random.nextBoolean() ? 0.5 : -0.5; // 随机向前或向后
        }
        if (rotateDuration <= 0) {
            rotateDuration = random.nextInt(50) + 20; // 随机旋转20 - 70帧
            rotateStep = random.nextBoolean() ? Math.toRadians(3) : -Math.toRadians(3); // 随机左右旋转
        }

        // 旋转
        angle += rotateStep;
        // 确保角度在 0 - 2π 范围内
        angle = (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);

        // 移动
        int newX = (int) (x + moveSpeed * Math.sin(angle));
        int newY = (int) (y - moveSpeed * Math.cos(angle));
        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            // 碰撞时改变方向
            moveSpeed = -moveSpeed;
        }

        moveDuration--;
        rotateDuration--;
    }

    public Image getTankImage() {
        return tankImage;
    }

    public double getAngle() {
        return angle;
    }

    public void update() {
        if (!alive) return;  // 如果坦克死亡不再更新
        updateMovement();
        updateBullets();
        tryToFire();
    }

    private void tryToFire() {
        if (!alive) return;  // 如果坦克已死亡，不再发射子弹
        // 随机决定是否射击
        if (random.nextInt(FIRE_CHANCE) == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFireTime >= FIRE_INTERVAL) {
                fire();
                lastFireTime = currentTime;
            }
        }
    }

    @Override
    public void fire() {
        // 计算子弹发射位置(坦克前方)
        int bulletX = (int) (x + width/2 + (width/2 + 5) * Math.sin(angle));
        int bulletY = (int) (y + height/2 - (height/2 + 5) * Math.cos(angle));

        EnemyBullet bullet = new EnemyBullet(bulletX, bulletY, angle);
        bullets.add(bullet);
        // 设置子弹自动消失的定时器
        new Timer(BULLET_LIFETIME, e -> {
            bullet.deactivate();
            ((Timer)e.getSource()).stop();
        }).start();
    }

    public void updateBullets() {
        long currentTime = System.currentTimeMillis();
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            bullet.updatePosition();

            // 检查是否超出边界或已失活
            if (!bullet.isActive() || isOutOfBounds(bullet)) {
                bullets.remove(i);
                continue;
            }
        }
    }

    private boolean isOutOfBounds(EnemyBullet bullet) {
        Rectangle bounds = bullet.getCollisionBounds();
        if (bounds == null) return true;
        return bounds.x < 0 || bounds.y < 0 ||
                bounds.x > PVPModeWidth ||
                bounds.y > PVPModeHeight;
    }

    public void drawBullets(Graphics g) {
        for (EnemyBullet bullet : bullets) {
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
        if (normalizedAngle >= Math.PI / 4 && normalizedAngle < 3 * Math.PI / 4) {
            return 1; // 右
        } else if (normalizedAngle >= 3 * Math.PI / 4 && normalizedAngle < 5 * Math.PI / 4) {
            return 2; // 下
        } else if (normalizedAngle >= 5 * Math.PI / 4 && normalizedAngle < 7 * Math.PI / 4) {
            return 3; // 左
        } else {
            return 0; // 上
        }
    }

    @Override
    public void takeDamage(int damage) {
        alive = false;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Rectangle getCollisionBounds() {
        if (!alive) return null;
        return new Rectangle(x, y, width, height);
    }

    public List<EnemyBullet> getBullets() {
        if (bullets == null) {
            bullets = new ArrayList<>();
        }
        return bullets;
    }
}