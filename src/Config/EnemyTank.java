package src.Config;

import src.InterFace.CollisionDetector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static src.Structure.ModeCardLayOut.PVPModeHeight;
import static src.Structure.ModeCardLayOut.PVPModeWidth;

public class EnemyTank extends AbstractTank {
    private boolean alive = true;
    private double angle = 0; // 当前朝向角度，0为向右，逆时针为正（数学坐标系）
    private final String tankPath = "/src/Images/TankImage/EnemyTank/tankR.gif";
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
    private int lastPlayerX = 0;
    private int lastPlayerY = 0;

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

    // 巡逻模式,尝试移动到指定点
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

    // 避开墙壁模式,当发生碰撞时尝试改变方向
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

    // 智能射击,考虑玩家位置
    private void tryToFire(PlayerTank player) {
        if (!alive || player == null || !player.isAlive()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime < FIRE_INTERVAL) {
            return; // 冷却中
        }

        // 计算与玩家的角度
        double playerCenterX = player.getX() + player.getWidth()/2;
        double playerCenterY = player.getY() + player.getHeight()/2;
        double tankCenterX = x + width/2;
        double tankCenterY = y + height/2;
        
        double playerAngle = Math.atan2(
                playerCenterY - tankCenterY,
                playerCenterX - tankCenterX
        );
        // 标准化角度
        playerAngle = (playerAngle + Math.PI/2) % (2 * Math.PI);
        if (playerAngle < 0) playerAngle += 2 * Math.PI;

        // 计算角度差
        double angleDiff = Math.abs(playerAngle - angle);
        if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;

        // 计算与玩家的距离
        double distance = Math.sqrt(
                Math.pow(playerCenterX - tankCenterX, 2) +
                Math.pow(playerCenterY - tankCenterY, 2)
        );

        // 当玩家在坦克前方一定角度范围内时提高射击概率
        int fireProb;
        if (angleDiff < Math.PI / 6) { // 前方30度范围内
            // 距离越近，射击概率越高
            if (distance < 150) {
                fireProb = 3; // 1/3概率
            } else if (distance < 300) {
                fireProb = 5; // 1/5概率
            } else {
                fireProb = 10; // 1/10概率
            }
        } else if (angleDiff < Math.PI / 4) { // 前方45度范围内
            if (distance < 200) {
                fireProb = 5; // 1/5概率
            } else {
                fireProb = 12; // 1/12概率
            }
        } else {
            // 玩家不在正前方，降低射击概率
            fireProb = 20; // 1/20概率
        }

        // 尝试射击
        if (random.nextInt(fireProb) == 0) {
            // 计算预测射击
            double predictFactor = 0.0;
            
            // 只有当玩家移动时才进行预测
            if (Math.abs(player.getX() - lastPlayerX) > 2 || Math.abs(player.getY() - lastPlayerY) > 2) {
                // 预测玩家移动方向
                double moveAngle = Math.atan2(
                    player.getY() - lastPlayerY,
                    player.getX() - lastPlayerX
                );
                
                // 预测因子 - 根据距离和速度调整
                double playerSpeed = Math.sqrt(
                    Math.pow(player.getX() - lastPlayerX, 2) + 
                    Math.pow(player.getY() - lastPlayerY, 2)
                );
                
                // 简单预测 - 距离越远预测越多
                predictFactor = (distance / 400) * (playerSpeed / 5) * 0.1;
                
                // 应用预测调整到射击角度
                angle = playerAngle + predictFactor;
            } else {
                // 玩家静止，直接瞄准
                angle = playerAngle;
            }
            
            // 发射子弹
            fire(player);
            
            // 更新上次玩家位置（用于下次预测）
            lastPlayerX = player.getX();
            lastPlayerY = player.getY();
        }
    }

    @Override
    public void fire() {
        // 基本射击，没有目标
        fire(null);
    }

    @Override
    public void fire(PlayerTank player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime >= FIRE_INTERVAL) {
            // 计算炮管前端位置,正确的计算方式
            int barrelLength = width / 2 + 5;

            int bulletX = (int) (x + width / 2 + Math.cos(angle) * barrelLength);
            int bulletY = (int) (y + height / 2 + Math.sin(angle) * barrelLength);

            // 检查子弹生成位置是否与玩家坦克重叠
            if (player != null && player.isAlive()) {
                Rectangle bulletBounds = new Rectangle(bulletX-5, bulletY-5, 10, 10);
                if (player.getCollisionBounds().intersects(bulletBounds)) {
                    return; // 取消本次射击，防止子弹在玩家内部生成
                }
            }

            // 根据坦克难度添加随机偏移
            double randomSpread = (random.nextDouble() - 0.5) * 0.2;
            double fireAngle = angle + randomSpread;
            
            // 创建子弹
            EnemyBullet bullet = new EnemyBullet(bulletX, bulletY, fireAngle);
            bullet.setMinCollisionDistance(30); // 设置最小碰撞检测距离
            bullets.add(bullet);
            lastFireTime = currentTime;
        }
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
        if (alive && health > 0) {
            health -= damage;

            if (health <= 0) {
                // 保存当前位置用于爆炸效果
                final int explosionX = x + width / 2;
                final int explosionY = y + height / 2;
                final int explosionSize = Math.max(width, height) * 2;

                // 记录子弹并转移到孤儿子弹列表
                List<EnemyBullet> activeBullets = new ArrayList<>(bullets);
                bullets.clear();

                // 标记为死亡
                alive = false;

                // 创建爆炸效果
                ExplosionManager.getInstance().createExplosion(explosionX, explosionY, explosionSize);

            }
        }
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

    @Override
    protected void drawTank(Graphics g) {
        if (tankImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int centerX = x + width / 2;
            int centerY = y + height / 2;

            g2d.translate(centerX, centerY);
            g2d.rotate(angle);
            g2d.drawImage(tankImage, -width / 2, -height / 2, width, height, null);

            g2d.dispose();
        }
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g); // 确保调用父类的draw方法

        // 只有在坦克存活时才绘制坦克图像和子弹
        if (isAlive()) {
            // 绘制子弹
            drawBullets(g);
        }
    }
}
