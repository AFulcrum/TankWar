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
    private double learningRate = 0.1;
    private Map<String, Double> weights;
    private static final String SAVE_FILE = "AITankData.dat";
    private int level;
    private long lastActionTime;
    private static final long ACTION_DELAY = 500;
    private List<EnemyBullet> bullets;
    private double angle = 0;  // 添加角度属性
    private Image tankImage;   // 添加坦克图片

    public AITank(int x, int y, CollisionDetector detector) {
        super(x, y, 32, 32, 1, detector);
        this.weights = loadLearnedData();
        this.lastActionTime = System.currentTimeMillis();
        this.bullets = new ArrayList<>();
        loadTankImage();
    }
    private void loadTankImage() {
        try {
            String path = "/Images/TankImage/EnemyTank/tankU.gif";  // 使用红色坦克图片
            URL url = getClass().getResource(path);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                tankImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            System.err.println("无法加载AI坦克图像: " + e.getMessage());
        }
    }

    public void updateAI(PlayerTank player, int currentLevel) {
        if (!isAlive() || player == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < ACTION_DELAY) return;
        lastActionTime = currentTime;

        // 基础AI参数
        double baseAccuracy = 0.1 + (currentLevel * 0.05);
        double baseMoveSpeed = 1 + (currentLevel * 0.2);

        // 获取与玩家相关的状态
        double distanceToPlayer = calculateDistance(player);
        double angleToPlayer = calculateAngle(player);

        // 使用学习权重调整行为
        double shootingProb = weights.getOrDefault("shooting", 0.5) * baseAccuracy;
        double moveSpeed = weights.getOrDefault("speed", 1.0) * baseMoveSpeed;

        // 更新位置
        updatePosition(player, moveSpeed);

        // 根据学习结果决定是否射击
        if (Math.random() < shootingProb) {
            fire();
        }

        if (System.currentTimeMillis() - lastActionTime >= ACTION_DELAY) {
            // 更新角度以面向玩家
            angle = calculateAngleToPlayer(player);

            // 根据难度级别调整移动速度
            double move = 1 + (currentLevel * 0.5);
            updatePosition(player, move);

            // 根据难度决定是否开火
            if (Math.random() < 0.1 * currentLevel) {
                fire();
            }

            lastActionTime = System.currentTimeMillis();
        }

        // 更新子弹
        updateBullets();
    }

    private void updatePosition(PlayerTank player, double moveSpeed) {
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 200) {  // 如果距离玩家太远，就靠近
            int newX = (int)(x + (dx/distance) * moveSpeed);
            int newY = (int)(y + (dy/distance) * moveSpeed);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
        else if (distance < 100) {  // 如果距离玩家太近，就远离
            int newX = (int)(x - (dx/distance) * moveSpeed);
            int newY = (int)(y - (dy/distance) * moveSpeed);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
        else {  // 在玩家附近时随机移动
            double angle = Math.random() * 2 * Math.PI;  // 随机角度
            int newX = (int)(x + Math.cos(angle) * moveSpeed);
            int newY = (int)(y + Math.sin(angle) * moveSpeed);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }

    public void learn(boolean success) {
        // 根据成功或失败调整权重
        double adjustment = success ? learningRate : -learningRate;

        weights.merge("shooting", adjustment, Double::sum);
        weights.merge("speed", adjustment, Double::sum);

        // 确保权重在合理范围内
        normalizeWeights();
    }

    private void normalizeWeights() {
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            double value = entry.getValue();
            value = Math.max(0.1, Math.min(2.0, value));
            entry.setValue(value);
        }
    }

    public void saveLearnedData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("Config/" + SAVE_FILE))) {
            oos.writeObject(weights);
        } catch (IOException e) {
            System.err.println("无法保存AI学习数据：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> loadLearnedData() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("Config/" + SAVE_FILE))) {
            return (Map<String, Double>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // 如果文件不存在或出错，返回默认权重
            Map<String, Double> defaultWeights = new HashMap<>();
            defaultWeights.put("shooting", 0.5);
            defaultWeights.put("speed", 1.0);
            return defaultWeights;
        }
    }

    private double calculateDistance(PlayerTank player) {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double calculateAngle(PlayerTank player) {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        return Math.atan2(dy, dx);
    }

    @Override
    public void fire() {
        if (System.currentTimeMillis() - lastActionTime >= ACTION_DELAY) {
            EnemyBullet bullet = new EnemyBullet(
                    getX() + getWidth()/2,
                    getY() + getHeight()/2,
                    angle
            );
            bullets.add(bullet);
            lastActionTime = System.currentTimeMillis();
        }
    }

    private double calculateAngleToPlayer(PlayerTank player) {
        double dx = player.getX() - getX();
        double dy = player.getY() - getY();
        return Math.atan2(dy, dx);
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

    public List<EnemyBullet> getBullets() {
        return bullets != null ? bullets : new ArrayList<>();
    }

    public void drawBullets(Graphics g) {
        // 移除失效的子弹
        bullets.removeIf(bullet -> !bullet.isActive());

        // 绘制所有活跃的子弹
        for (EnemyBullet bullet : bullets) {
            bullet.draw(g);
        }
    }
    public void updateBullets() {
        // 更新所有子弹的位置
        for (EnemyBullet bullet : bullets) {
            bullet.updatePosition();
        }
    }
    @Override
    public double getAngle() {
        return angle;
    }

    public Image getCurrentImage() {
        return tankImage;
    }
}