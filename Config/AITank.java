package Config;

import InterFace.CollisionDetector;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AITank extends AbstractTank {
    // 学习率控制学习速度
    private double learningRate = 0.5;
    // 衰减因子控制旧经验的权重
    private double decayFactor = 0.5;
    // 保存学习数据的权重表
    private Map<String, Double> weights;
    private static final String DATA_DIR = System.getProperty("user.dir")
            + File.separator + "TankWar"
            + File.separator + "Data";

    private static final String SAVE_FILE = "AITankData.dat";


    private long lastActionTime;
    private static final long ACTION_DELAY = 100;
    private List<EnemyBullet> bullets;
    private double angle = 0;
    private Image tankImage;
    // 新增学习数据
    private Map<String, Double> playerPatterns; // 记录玩家行为模式
    private long lastPlayerPositionUpdate;
    private static final long PATTERN_UPDATE_INTERVAL = 1000;
    private static final int BASE_SPEED =9; // 基础速度
    private int currentSpeed = BASE_SPEED;

    public AITank(int x, int y, CollisionDetector detector) {
        super(x, y, 64, 64, 1, detector);
        createDataDirectory();
        this.weights = loadLearnedData();
        this.playerPatterns = new HashMap<>();
        this.lastActionTime = System.currentTimeMillis();
        this.lastPlayerPositionUpdate = System.currentTimeMillis();
        this.bullets = new ArrayList<>();
        loadTankImage();
    }
    private void createDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (!created) {
                System.err.println("无法创建数据目录: " + DATA_DIR);
            }
        }
    }

    private void loadTankImage() {
        try {
            URL url = getClass().getResource("/Images/TankImage/EnemyTank/tankU.gif");
            if (url == null) {
                System.err.println("AI坦克图片不存在");
                tankImage = null;
                return;
            }
            ImageIcon icon = new ImageIcon(url);
            tankImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.err.println("无法加载AI坦克图像: " + e.getMessage());
        }
    }

    // 新增方法：更新玩家行为模式
    private void updatePlayerPatterns(PlayerTank player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerPositionUpdate < PATTERN_UPDATE_INTERVAL) return;

        // 记录玩家移动方向频率
        String patternKey = "move_" + player.getDirection();
        playerPatterns.merge(patternKey, 1.0, Double::sum);

        // 记录玩家射击频率
        if (player.isShooting()) {
            playerPatterns.merge("shoot_freq", 1.0, Double::sum);
        }

        lastPlayerPositionUpdate = currentTime;
    }

    // 增强的AI更新方法
    public void updateAI(PlayerTank player, int currentLevel) {
        if (!isAlive() || player == null) return;

        updatePlayerPatterns(player);
        long currentTime = System.currentTimeMillis();

        // 动态调整行为延迟(随关卡减少)
        long dynamicDelay = (long)(ACTION_DELAY * (1 - currentLevel * 0.03));
        if (currentTime - lastActionTime < dynamicDelay) return;

        // 根据当前级别调整难度因子
        double levelFactor = 1 + (currentLevel * 0.15);

        // 计算与玩家的相对位置
        double distance = calculateDistance(player);
        double angleToPlayer = calculateAngleToPlayer(player);
        this.angle = angleToPlayer; // 面向玩家

        // 根据威胁级别动态调整速度
        double threatLevel = calculateThreatLevel(player);
        currentSpeed = (int)(BASE_SPEED * (1 + threatLevel * 0.5));

        // 躲避子弹行为
        if (shouldEvadeBullets(player)) {
            evadeBullets(player, levelFactor);
        }
        // 根据距离决定主要行为
        else if (distance > 400) {
            predictiveChase(player, levelFactor); // 预测性追击
        } else if (distance < 200) {
            smartEvade(player, levelFactor); // 智能躲避
        } else {
            tacticalMovement(player, levelFactor); // 战术移动
        }

        // 智能射击决策
        if (shouldShoot(player, distance, angleToPlayer, levelFactor)) {
            fire();
        }

        updateBullets();
        lastActionTime = currentTime;
    }
    private double calculateThreatLevel(PlayerTank player) {
        double threat = 0;

        // 玩家距离越近威胁越大
        double distance = calculateDistance(player);
        threat += Math.max(0, 1 - distance / 300);

        // 玩家子弹越近威胁越大
        for (PlayerBullet bullet : player.getBullets()) {
            double bulletDistance = Math.sqrt(Math.pow(bullet.getX() - x, 2) + Math.pow(bullet.getY() - y, 2));
            if (bulletDistance < 150) {
                threat += (1 - bulletDistance / 150) * 2; // 子弹威胁权重更高
            }
        }

        return Math.min(threat, 1.5); // 限制最大威胁级别
    }

    private void tacticalMovement(PlayerTank player, double levelFactor) {
        // 计算与玩家的相对位置
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 根据玩家位置调整移动策略
        if (distance > 250) {
            moveToward(player, levelFactor); // 向玩家移动
        } else if (distance < 150) {
            evadeFrom(player, levelFactor); // 躲避玩家
        } else {
            randomMove(levelFactor); // 随机移动
        }
    }

    private void smartEvade(PlayerTank player, double levelFactor) {
        // 计算玩家子弹的威胁位置
        for (PlayerBullet bullet : player.getBullets()) {
            double bulletDistance = Math.sqrt(Math.pow(bullet.getX() - x, 2) + Math.pow(bullet.getY() - y, 2));
            if (bulletDistance < 250) { // 如果子弹距离小于100像素，触发躲避
                evadeFrom(player, levelFactor);
                return;
            }
        }

        // 如果没有威胁，随机移动
        randomMove(levelFactor);
    }

    private void evadeBullets(PlayerTank player, double levelFactor) {
        // 计算玩家子弹的威胁位置
        for (PlayerBullet bullet : player.getBullets()) {
            double bulletDistance = Math.sqrt(Math.pow(bullet.getX() - x, 2) + Math.pow(bullet.getY() - y, 2));
            if (bulletDistance < 250) { // 如果子弹距离小于100像素，触发躲避
                evadeFrom(player, levelFactor);
                return;
            }
        }
    }

    private boolean shouldEvadeBullets(PlayerTank player) {
        if (!player.isShooting()) return false;

        // 检查所有玩家子弹，增加检测范围
        for (PlayerBullet bullet : player.getBullets()) {
            double bulletDistance = Math.sqrt(Math.pow(bullet.getX() - x, 2) + Math.pow(bullet.getY() - y, 2));
            if (bulletDistance < 200) { // 从100增加到200像素检测范围
                // 计算子弹方向与AI坦克的夹角
                double bulletAngle = Math.atan2(bullet.getY() - y, bullet.getX() - x);
                double angleDiff = Math.abs(normalizeAngle(angle - bulletAngle));

                // 如果子弹大致朝向AI坦克，即使距离稍远也触发躲避
                if (angleDiff < Math.PI/4 || bulletDistance < 100) {
                    return true;
                }
            }
        }
        return false;
    }

    // 预测性追击
    private void predictiveChase(PlayerTank player, double speedFactor) {
        // 预测玩家移动方向
        double predictDistance = 50 * speedFactor;
        double predictX = player.getX() + Math.cos(player.getAngle()) * predictDistance;
        double predictY = player.getY() + Math.sin(player.getAngle()) * predictDistance;

        // 向预测位置移动
        double dx = predictX - x;
        double dy = predictY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            this.angle = Math.atan2(dy, dx);
            int newX = (int)(x + (dx/distance) * speedFactor);
            int newY = (int)(y + (dy/distance) * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }

    //智能射击决策
    private boolean shouldShoot(PlayerTank player, double distance, double angleToPlayer, double levelFactor) {
        // 基础射击概率
        double baseProb = 0.3 * levelFactor;

        // 距离因素,中等距离时命中率更高
        double distanceFactor = 1 - Math.abs(distance - 250) / 250;

        // 角度因素,正对玩家时命中率更高
        double angleDiff = Math.abs(normalizeAngle(angle - angleToPlayer));
        double angleFactor = 1 - angleDiff / Math.PI;

        // 综合射击概率
        double shootProb = baseProb * (0.4 + 0.3 * distanceFactor + 0.3 * angleFactor);

        return Math.random() < shootProb;
    }

    // 标准化角度到[0, 2π]
    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }

    public void updateBullets() {
        // 移除失效的子弹
        bullets.removeIf(bullet -> !bullet.isActive());

        // 更新剩余子弹的位置
        for (EnemyBullet bullet : bullets) {
            bullet.updatePosition();
        }
    }
    // 死亡时的学习加强
    public void onDeath(PlayerTank player) {
        // 死亡时进行强化学习
        double deathPenalty = -0.5; // 死亡惩罚
        learn(false, player);
        // 额外更新权重
        updateBasicWeights(deathPenalty * 2);
        updatePositionBasedLearning(player, deathPenalty);
        updatePatternRecognition(deathPenalty);
        // 保存学习数据
        saveLearnedData();
    }

    private double calculateAngleToPlayer(PlayerTank player) {
        if (player == null) return 0;
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        return Math.atan2(dy, dx);
    }

    private double calculateDistance(PlayerTank player) {
        if (player == null) return 0;
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // 向玩家移动
    private void moveToward(PlayerTank player, double speedFactor) {
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // 更新移动方向的同时更新角度
            this.angle = Math.atan2(dy, dx);

            int newX = (int)(x + (dx/distance) * speedFactor);
            int newY = (int)(y + (dy/distance) * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }

    //躲避玩家
    private void evadeFrom(PlayerTank player, double speedFactor) {
        // 优先躲避子弹
        PlayerBullet nearestBullet = findNearestBullet(player);
        if (nearestBullet != null) {
            // 计算垂直于子弹方向的躲避方向
            double bulletAngle = Math.atan2(nearestBullet.getY() - y, nearestBullet.getX() - x);
            double evadeAngle = bulletAngle + (Math.random() > 0.5 ? Math.PI/2 : -Math.PI/2);

            int newX = (int)(x + Math.cos(evadeAngle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(evadeAngle) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
                this.angle = evadeAngle; // 调整面向方向
                return;
            }
        }

        // 如果没有子弹威胁，躲避玩家
        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            int newX = (int)(x + (dx/distance) * currentSpeed * speedFactor);
            int newY = (int)(y + (dy/distance) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
                this.angle = Math.atan2(dy, dx); // 调整面向方向
            }
        }
    }
    //寻找最近的子弹
    private PlayerBullet findNearestBullet(PlayerTank player) {
        PlayerBullet nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (PlayerBullet bullet : player.getBullets()) {
            double distance = Math.sqrt(Math.pow(bullet.getX() - x, 2) + Math.pow(bullet.getY() - y, 2));
            if (distance < minDistance) {
                minDistance = distance;
                nearest = bullet;
            }
        }

        return nearest;
    }

    //随机移动
    private void randomMove(double speedFactor) {
        double angle = Math.random() * 2 * Math.PI;
        int newX = (int)(x + Math.cos(angle) * speedFactor);
        int newY = (int)(y + Math.sin(angle) * speedFactor);

        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
        }
    }

    // 学习方法
    public void learn(boolean success, PlayerTank player) {
        // 动态调整学习率(随关卡提升)
        double dynamicLearningRate = learningRate * (1 + ConfigTool.getLevel() * 0.5);

        // 根据成功或失败调整权重
        double adjustment = success ? dynamicLearningRate : -dynamicLearningRate * 0.5;

        // 更新基础行为权重
        updateBasicWeights(adjustment);

        // 更新基于玩家位置的学习
        updatePositionBasedLearning(player, adjustment);

        // 更新玩家行为模式识别
        updatePatternRecognition(adjustment);

        normalizeWeights();
    }
    private void updateBasicWeights(double adjustment) {
        weights.merge("chase", adjustment, (oldVal, newVal) ->
                oldVal * decayFactor + newVal * (1 - decayFactor));
        weights.merge("evade", adjustment, (oldVal, newVal) ->
                oldVal * decayFactor + newVal * (1 - decayFactor));
        weights.merge("shoot", adjustment, (oldVal, newVal) ->
                oldVal * decayFactor + newVal * (1 - decayFactor));
    }

    private void updatePositionBasedLearning(PlayerTank player, double adjustment) {
        // 根据相对位置学习
        double relX = player.getX() - this.x;
        double relY = player.getY() - this.y;
        String posKey = "pos_" + (relX > 0 ? "R" : "L") + (relY > 0 ? "D" : "U");
        weights.merge(posKey, adjustment, Double::sum);
    }

    private void updatePatternRecognition(double adjustment) {
        // 更新玩家模式识别权重
        playerPatterns.forEach((key, value) ->
                weights.merge("pattern_" + key, value * adjustment, Double::sum));
    }

    private void normalizeWeights() {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return; // 防止除以零

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            weights.put(entry.getKey(), entry.getValue() / total);
        }
    }

    // 增强的保存方法
    public void saveLearnedData() {
        File saveFile = new File(DATA_DIR, SAVE_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(saveFile))) {
            oos.writeObject(weights);
            System.out.println("AI数据已保存到: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存AI数据失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> loadLearnedData() {
        File file = new File(DATA_DIR, SAVE_FILE);
        if (!file.exists()) {
            System.out.println("未找到AI数据文件,创建默认权重");
            return createDefaultWeights();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file))) {
            Map<String, Object> saveData = (Map<String, Object>) ois.readObject();

            if (saveData.containsKey("weights")) {
                return (Map<String, Double>) saveData.get("weights");
            }
        } catch (Exception e) {
            System.err.println("加载AI数据失败: " + e.getMessage());
        }
        return createDefaultWeights();
    }

    // 改进默认权重设置
    private Map<String, Double> createDefaultWeights() {
        Map<String, Double> defaults = new HashMap<>();
        // 设置更激进的初始值
        defaults.put("chase", 0.8);    // 追击倾向
        defaults.put("evade", 0.6);    // 躲避倾向
        defaults.put("shoot", 0.7);    // 射击倾向
        defaults.put("pattern_recognition", 0.5); // 模式识别
        // 添加新的策略权重
        defaults.put("aggressive", 0.7);  // 激进程度
        defaults.put("defensive", 0.6);   // 防御程度
        return defaults;
    }

    @Override
    public void fire() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= FIRE_INTERVAL) {
            // 计算炮管末端位置(从坦克前方射出)
            int barrelLength = width / 2 + 10; // 炮管长度
            int bulletX = (int) (x + width / 2 + Math.cos(angle) * barrelLength);
            int bulletY = (int) (y + height / 2 + Math.sin(angle) * barrelLength);

            // 创建子弹时加入预测偏移
            double predictFactor = weights.getOrDefault("predict", 0.3);
            double predictAngle = angle + (Math.random() - 0.5) * predictFactor;

            EnemyBullet bullet = new EnemyBullet(bulletX, bulletY, predictAngle);

            bullets.add(bullet);
            lastActionTime = currentTime;
        }
    }

    @Override
    public void useSkill() {

    }

    @Override
    public int getDirection() {
        return 0;
    }

    @Override
    public Rectangle getCollisionBounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public void revive() {
        this.alive = true;
        this.health = 1; // 重置生命值
    }

    public void setAlive(boolean b) {
        this.alive = b;
    }

    public List<EnemyBullet> getBullets() {
        if (bullets == null) {
            bullets = new ArrayList<>();
        }
        return bullets;
    }

    public void drawBullets(Graphics g) {
        for (EnemyBullet bullet : bullets) {
            bullet.draw(g);
        }
    }
    @Override
    public Image getCurrentImage() {
        return tankImage;
    }
    @Override
    public double getAngle() {
        return this.angle;
    }
}