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
    private double moveSpeed = 9; // 移动速度
    private double rotateStep = Math.toRadians(6); // 旋转步长，每次转动6度
    private int moveDuration = 0; // 移动持续帧数
    private int rotateDuration = 0; // 旋转持续帧数
    private List<EnemyBullet> bullets;
    private long lastFireTime = 0;
    private static final int FIRE_CHANCE = 44; // 每帧有1/44的几率射击
    private static final int FIRE_INTERVAL = 2000; // 射击间隔2秒
    private static final int BULLET_LIFETIME = 10000; // 子弹最大存活时间10秒

    // 新增移动模式状态
    private enum MovementMode {
        RANDOM, // 随机移动
        PATROL, // 巡逻模式
        AVOID_WALL // 避开墙壁
    }

    private MovementMode currentMode = MovementMode.RANDOM;
    private int modeChangeCooldown = 0;
    private int patrolPointX = 0;
    private int patrolPointY = 0;
    private int stuckCounter = 0;
    private int lastX = 0;
    private int lastY = 0;

    public EnemyTank(int x, int y, CollisionDetector collisionDetector) {
        super(x, y, 66, 66, 1, collisionDetector); // 敌方坦克1滴血
        loadTankImage();
        this.bullets = new ArrayList<>();
        this.lastX = x;
        this.lastY = y;
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
        // 检测是否卡住
        if (Math.abs(x - lastX) < 1 && Math.abs(y - lastY) < 1) {
            stuckCounter++;
            if (stuckCounter > 20) { // 连续20帧未移动，认为卡住了
                changeMovementMode(MovementMode.AVOID_WALL);
                stuckCounter = 0;
            }
        } else {
            stuckCounter = 0;
        }

        lastX = x;
        lastY = y;

        // 随机切换移动模式
        if (modeChangeCooldown <= 0) {
            int randValue = random.nextInt(100);
            if (randValue < 60) {
                changeMovementMode(MovementMode.RANDOM);
            } else if (randValue < 90) {
                changeMovementMode(MovementMode.PATROL);
                // 设置随机巡逻点
                patrolPointX = random.nextInt(PVPModeWidth - 100) + 50;
                patrolPointY = random.nextInt(PVPModeHeight - 100) + 50;
            } else {
                changeMovementMode(MovementMode.AVOID_WALL);
            }
            modeChangeCooldown = random.nextInt(150) + 100; // 100-250帧后再切换模式
        }
        modeChangeCooldown--;

        // 根据当前模式执行不同的移动策略
        switch (currentMode) {
            case RANDOM:
                randomMovement();
                break;
            case PATROL:
                patrolMovement();
                break;
            case AVOID_WALL:
                avoidWallMovement();
                break;
        }
    }

    private void changeMovementMode(MovementMode newMode) {
        currentMode = newMode;
        // 重置移动参数
        moveDuration = random.nextInt(100) + 50;
        rotateDuration = random.nextInt(50) + 20;
    }

    // 随机移动模式
    private void randomMovement() {
        // 随机决定是否开始新的移动或旋转
        if (moveDuration <= 0) {
            moveDuration = random.nextInt(100) + 50; // 随机移动50 - 150帧
            moveSpeed = random.nextDouble() * 0.6 + 0.2; // 0.2-0.8的随机速度
            if (random.nextBoolean()) {
                moveSpeed = -moveSpeed; // 50%概率后退
            }
        }
        if (rotateDuration <= 0) {
            rotateDuration = random.nextInt(50) + 20; // 随机旋转20 - 70帧
            rotateStep = (random.nextDouble() * 3 + 1) * Math.PI / 180; // 1-4度的随机旋转
            if (random.nextBoolean()) {
                rotateStep = -rotateStep; // 50%概率向左转
            }
        }

        // 旋转
        angle += rotateStep;
        angle = (angle + 2 * Math.PI) % (2 * Math.PI);

        // 移动
        moveForward();

        moveDuration--;
        rotateDuration--;
    }

    // 巡逻模式 - 尝试移动到指定点
    private void patrolMovement() {
        // 计算与目标点的方向
        double targetAngle = Math.atan2(
                patrolPointX - (x + width/2),
                -(patrolPointY - (y + height/2))
        );
        targetAngle = (targetAngle + 2 * Math.PI) % (2 * Math.PI);

        // 计算需要旋转的角度差
        double angleDiff = targetAngle - angle;
        // 标准化到[-π, π]
        if (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        if (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        // 根据角度差旋转
        double rotationAmount = Math.min(Math.abs(angleDiff), Math.toRadians(3));
        if (angleDiff > 0) {
            angle += rotationAmount;
        } else {
            angle -= rotationAmount;
        }
        angle = (angle + 2 * Math.PI) % (2 * Math.PI);

        // 只有当朝向大致正确时才移动
        if (Math.abs(angleDiff) < Math.PI / 6) { // 30度内
            moveSpeed = 0.7; // 巡逻模式速度稍快
            moveForward();
        }

        // 检查是否已到达目标点附近
        double distanceToTarget = Math.sqrt(
                Math.pow(patrolPointX - (x + width/2), 2) +
                        Math.pow(patrolPointY - (y + height/2), 2)
        );

        if (distanceToTarget < 30) {
            // 已到达目标点附近，重新设置巡逻点或切换模式
            if (random.nextBoolean()) {
                patrolPointX = random.nextInt(PVPModeWidth - 100) + 50;
                patrolPointY = random.nextInt(PVPModeHeight - 100) + 50;
            } else {
                changeMovementMode(MovementMode.RANDOM);
            }
        }
    }

    // 避开墙壁模式 - 当发生碰撞时尝试改变方向
    private void avoidWallMovement() {
        // 探测前方是否有墙
        int probeDistance = 30;
        int probeX = (int)(x + width/2 + probeDistance * Math.sin(angle));
        int probeY = (int)(y + height/2 - probeDistance * Math.cos(angle));

        boolean wallAhead = false;
        if (collisionDetector != null) {
            wallAhead = collisionDetector.isColliding(probeX-5, probeY-5, 10, 10);
        }

        if (wallAhead) {
            // 前方有墙，随机选择新方向
            angle += Math.toRadians(random.nextInt(120) + 30); // 转30-150度
            angle = (angle + 2 * Math.PI) % (2 * Math.PI);
        } else {
            // 前方无墙，正常前进
            moveSpeed = 0.5;
            moveForward();
        }

        // 一段时间后恢复正常移动
        moveDuration--;
        if (moveDuration <= 0) {
            changeMovementMode(MovementMode.RANDOM);
        }
    }

    // 向当前角度方向移动
    private void moveForward() {
        int newX = (int) (x + moveSpeed * Math.sin(angle));
        int newY = (int) (y - moveSpeed * Math.cos(angle));

        // 尝试分步移动，提高通过能力
        boolean canMoveX = !checkCollision(newX, y);
        boolean canMoveY = !checkCollision(x, newY);

        if (!canMoveX && !canMoveY) {
            // 两个方向都不能移动，可能卡住了
            stuckCounter += 5; // 加速识别卡住状态
            return;
        }

        // 更新位置
        if (!canMoveX && canMoveY) {
            // 只能垂直移动
            y = newY;
        } else if (canMoveX && !canMoveY) {
            // 只能水平移动
            x = newX;
        } else {
            // 两个方向都能移动
            x = newX;
            y = newY;
        }
    }

    // 检查移动位置是否有碰撞
    public boolean checkCollision(int newX, int newY) {
        if (collisionDetector == null) return false;
        return collisionDetector.isColliding(newX, newY, width, height);
    }

    public Image getTankImage() {
        return tankImage;
    }

    public double getAngle() {
        return angle;
    }

    @Override
    public void revive() {

    }

    public void update() {
        if (!alive) return;
        updateMovement();
        updateBullets();
        tryToFire();
    }

    public void update(PlayerTank player) {
        if (!alive || player == null) return;
        updateMovement();
        updateBullets();
        tryToFire(player);
    }

    private void tryToFire() {
        if (!alive) return;
        // 随机决定是否射击
        if (random.nextInt(FIRE_CHANCE) == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFireTime >= FIRE_INTERVAL) {
                fire();
                lastFireTime = currentTime;
            }
        }
    }

    // 智能射击 - 考虑玩家位置
    private void tryToFire(PlayerTank player) {
        if (!alive || player == null || !player.isAlive()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime < FIRE_INTERVAL) {
            return; // 冷却中
        }

        // 计算与玩家的角度
        double playerAngle = Math.atan2(
                (player.getX() + player.getWidth()/2) - (x + width/2),
                -((player.getY() + player.getHeight()/2) - (y + height/2))
        );
        playerAngle = (playerAngle + 2 * Math.PI) % (2 * Math.PI);

        // 计算角度差
        double angleDiff = Math.abs(playerAngle - angle);
        angleDiff = Math.min(angleDiff, 2 * Math.PI - angleDiff);

        // 当玩家在坦克前方一定角度范围内时提高射击概率
        if (angleDiff < Math.PI / 4) { // 前方45度范围内
            // 计算与玩家的距离
            double distance = Math.sqrt(
                    Math.pow((player.getX() + player.getWidth()/2) - (x + width/2), 2) +
                            Math.pow((player.getY() + player.getHeight()/2) - (y + height/2), 2)
            );

            // 距离越近，射击概率越高
            int fireProb;
            if (distance < 150) {
                fireProb = 5; // 1/5概率
            } else if (distance < 300) {
                fireProb = 10; // 1/10概率
            } else {
                fireProb = 15; // 1/15概率
            }

            if (random.nextInt(fireProb) == 0) {
                fire();
                lastFireTime = currentTime;
            }
        } else {
            // 玩家不在正前方，使用普通随机射击
            if (random.nextInt(FIRE_CHANCE) == 0) {
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
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            bullet.updatePosition();

            // 检查是否超出边界或已失活
            if (!bullet.isActive() || isOutOfBounds(bullet)) {
                bullets.remove(i);
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
