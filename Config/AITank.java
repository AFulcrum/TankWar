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
import java.util.Random;

public class AITank extends AbstractTank {
    // 学习参数
    private double learningRate = 0.5;
    private double decayFactor = 0.5;
    private Map<String, Double> weights;
    private Map<String, Double> playerPatterns;
    
    // 文件路径
    private static final String DATA_DIR = System.getProperty("user.dir") + File.separator + "Data";
    private static final String SAVE_FILE = "AITankData.dat";
    
    // 时间控制
    private long lastActionTime;
    private long lastPlayerPositionUpdate;
    private static final long ACTION_DELAY = 100;
    private static final long PATTERN_UPDATE_INTERVAL = 1000;
    
    // 坦克属性
    private List<EnemyBullet> bullets;
    private double angle = 0;
    private Image tankImage;
    private int currentSpeed = BASE_SPEED;
    private static final int BASE_SPEED = 2;
    
    // 威胁检测距离
    private static final double BULLET_DETECT_DISTANCE = 2500;
    private static final double IMMEDIATE_EVADE_DISTANCE = 666;
    private static final double CRITICAL_BULLET_DISTANCE = 66;
    
    // AI性格特性
    private double aggressiveness = 0.7;  // 攻击性 (0-1)
    private double intelligence = 0.6;   // 智能程度 (0-1)
    private double precision = 0.8;      // 精准度 (0-1)
    
    // 性能优化
    private final Random random = new Random();
    private int lastPlayerX = 0;
    private int lastPlayerY = 0;
    private PlayerBullet cachedNearestBullet = null;
    private long lastBulletCheck = 0;
    
    // 行为状态
    private int currentBehaviorState = BEHAVIOR_NORMAL;
    private static final int BEHAVIOR_NORMAL = 0;
    private static final int BEHAVIOR_ATTACKING = 1;
    private static final int BEHAVIOR_EVADING = 2;
    private static final int BEHAVIOR_STRATEGIC = 3;
    private CollisionDetector detector;  // 而不是 private AbstractTank detector;

    public AITank(int x, int y, CollisionDetector detector) {
        super(x, y, 64, 64, 1, detector);
        createDataDirectory();
        this.weights = loadLearnedData();
        this.playerPatterns = new HashMap<>();
        this.lastActionTime = System.currentTimeMillis();
        this.lastPlayerPositionUpdate = System.currentTimeMillis();
        this.bullets = new ArrayList<>();
        this.angle = 0;
        loadTankImage();
        
        // 添加这一行，修复detector为null的问题
        this.detector = detector;
        
        // 根据加载的数据初始化性格特性
        initializePersonality();
    }
    
    /**
     * 初始化AI性格特性
     */
    private void initializePersonality() {
        this.aggressiveness = weights.getOrDefault("personality_aggressive", 0.7);
        this.intelligence = weights.getOrDefault("personality_intelligence", 0.6);
        this.precision = weights.getOrDefault("personality_precision", 0.8);
    }

    /**
     * 创建数据目录
     */
    private void createDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (!created) {
                System.err.println("无法创建数据目录: " + DATA_DIR);
            }
        }
    }

    /**
     * 加载坦克图片 - 修正使用面向上的图片
     */
    private void loadTankImage() {
        try {
            // 使用面向上的图片
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

    /**
     * 更新玩家行为模式学习
     */
    private void updatePlayerPatterns(PlayerTank player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerPositionUpdate < PATTERN_UPDATE_INTERVAL) return;

        // 记录移动方向模式
        String patternKey = "move_" + player.getDirection();
        playerPatterns.merge(patternKey, 1.0, Double::sum);

        // 记录射击模式
        if (player.isShooting()) {
            playerPatterns.merge("shoot_freq", 1.0, Double::sum);
        }
        
        // 记录相对位置偏好
        int relX = player.getX() - x;
        int relY = player.getY() - y;
        String posKey = "pos_" + (Math.abs(relX) > Math.abs(relY) ? 
                                 (relX > 0 ? "right" : "left") : 
                                 (relY > 0 ? "down" : "up"));
        playerPatterns.merge(posKey, 1.0, Double::sum);
        
        // 标准化模式数据
        normalizePatterns();
        
        // 更新状态
        lastPlayerPositionUpdate = currentTime;
        lastPlayerX = player.getX();
        lastPlayerY = player.getY();
    }
    
    /**
     * 标准化玩家模式数据
     */
    private void normalizePatterns() {
        double sum = playerPatterns.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        
        if (sum > 0) {
            playerPatterns.replaceAll((k, v) -> v / sum);
        }
    }

    /**
     * 增强的AI更新方法
     */
    public void updateAI(PlayerTank player, int currentLevel) {
        if (!isAlive() || player == null || !player.isAlive()) return;

        long currentTime = System.currentTimeMillis();
        
        // 优化1: 仅在必要时更新模式学习数据 (200ms一次)
        if (currentTime - lastPlayerPositionUpdate > 200) {
            updatePlayerPatterns(player);
            lastPlayerPositionUpdate = currentTime;
        }

        // 优化2: 动态调整行为更新频率
        long dynamicDelay = (long)(ACTION_DELAY * (1 - currentLevel * 0.03));
        if (currentTime - lastActionTime < dynamicDelay) {
            // 即使不执行主要行为更新，也要更新子弹
            updateBullets();
            return;
        }

        // 计算基本状态数据
        double distance = calculateDistance(player);
        double angleToPlayer = calculateAngleToPlayer(player);
        double threatLevel = calculateThreatLevel(player);
        
        // 根据威胁级别动态调整速度
        currentSpeed = Math.min(BASE_SPEED * 2, (int)(BASE_SPEED * (1 + threatLevel * 0.5)));

        // 批量状态更新
        updateFacingAngle(angleToPlayer, player);
        decideBehaviorState(player, distance, threatLevel);
        double levelFactor = 1 + (currentLevel * 0.15);
        
        // 根据状态执行行为
        executeBehavior(player, distance, levelFactor);

        // 智能射击决策
        if (shouldShoot(player, distance, angleToPlayer, levelFactor)) {
            fire(player);  // 传入玩家参数
        }

        updateBullets();
        lastActionTime = currentTime;
        
        // 随机学习 - 每500次更新学习一次(约10秒)
        if (random.nextInt(500) == 0) {
            boolean success = isPerformingWell(player);
            learn(success, player);
        }
    }

    private void executeBehavior(PlayerTank player, double distance, double levelFactor) {
        switch (currentBehaviorState) {
            case BEHAVIOR_NORMAL:
                normalBehavior(player, distance, levelFactor);
                break;
            case BEHAVIOR_ATTACKING:
                attackPlayer(player, distance, levelFactor);
                break;
            case BEHAVIOR_EVADING:
                evadeBullets(player, levelFactor);
                break;
            case BEHAVIOR_STRATEGIC:
                strategicMovement(player, levelFactor);
                break;
            default:
                normalBehavior(player, distance, levelFactor);
                break;
        }
    }

    /**
     * 判断AI表现是否良好
     */
    private boolean isPerformingWell(PlayerTank player) {
        // 基于健康状态比较
        boolean healthAdvantage = health > player.getHealth() / 100.0;
        
        // 基于子弹数量和位置判断
        int activeBullets = 0;
        for (EnemyBullet bullet : bullets) {
            if (bullet.isActive()) activeBullets++;
        }
        
        return healthAdvantage || activeBullets > 2;
    }

    /**
     * 决定当前行为状态
     */
    private void decideBehaviorState(PlayerTank player, double distance, double threatLevel) {
        // 重置缓存的最近子弹
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBulletCheck > 200) {
            cachedNearestBullet = null;
            lastBulletCheck = currentTime;
        }
        
        // 检查子弹威胁
        if (shouldEvadeBullets(player)) {
            currentBehaviorState = BEHAVIOR_EVADING;
            return;
        }
        
        // 检查生命值 - 低生命值时更保守
        if (health < 0.4) {
            // 生命值低时更可能躲避
            if (random.nextDouble() < 0.7) {
                currentBehaviorState = BEHAVIOR_EVADING;
                return;
            }
        }
        
        // 基于距离和威胁的智能决策
        if (distance < 150) {
            // 近距离策略
            if (threatLevel > 0.7) {
                // 高威胁时，根据性格特性决定攻击或躲避
                double aggressiveThreshold = 0.4 + (health * 0.3);
                currentBehaviorState = random.nextDouble() < aggressiveness * aggressiveThreshold ? 
                                      BEHAVIOR_ATTACKING : BEHAVIOR_EVADING;
            } else {
                // 低威胁时，积极攻击
                currentBehaviorState = BEHAVIOR_ATTACKING;
            }
        } else if (distance > 400) {
            // 远距离时智能追击
            currentBehaviorState = BEHAVIOR_ATTACKING;
        } else {
            // 中等距离时，选择策略行动
            if (random.nextDouble() < intelligence) {
                currentBehaviorState = BEHAVIOR_STRATEGIC;
            } else {
                // 根据当前优势情况决定
                boolean hasAdvantage = health > player.getHealth() / 100.0;
                currentBehaviorState = hasAdvantage ? BEHAVIOR_ATTACKING : BEHAVIOR_STRATEGIC;
            }
        }
        
        // 根据学习的权重微调决策
        adjustBehaviorBasedOnLearning();
    }
    
    /**
     * 根据学习数据微调行为
     */
    private void adjustBehaviorBasedOnLearning() {
        // 获取学习权重
        double attackWeight = weights.getOrDefault("chase", 0.5);
        double evadeWeight = weights.getOrDefault("evade", 0.5);
        double strategicWeight = weights.getOrDefault("strategic", 0.5);
        
        // 计算加权概率
        double totalWeight = attackWeight + evadeWeight + strategicWeight;
        double attackProb = attackWeight / totalWeight;
        double evadeProb = evadeWeight / totalWeight;
        
        // 有10%概率根据学习权重重新选择行为
        if (random.nextDouble() < 0.1) {
            double rand = random.nextDouble();
            if (rand < attackProb) {
                currentBehaviorState = BEHAVIOR_ATTACKING;
            } else if (rand < attackProb + evadeProb) {
                currentBehaviorState = BEHAVIOR_EVADING;
            } else {
                currentBehaviorState = BEHAVIOR_STRATEGIC;
            }
        }
    }
    
    /**
     * 更新面向角度 - 考虑预测和平滑转向
     */
    private void updateFacingAngle(double targetAngle, PlayerTank player) {
        // 根据AI精度和预测能力调整角度
        double angleDiff = normalizeAngle(targetAngle - this.angle);
        
        // 智能预测移动方向
        if (intelligence > 0.5 && player.isMoving()) {
            // 预测玩家下一位置
            double predictScale = 0.2 + intelligence * 0.3;
            double dx = player.getX() - lastPlayerX;
            double dy = player.getY() - lastPlayerY;
            
            // 调整目标角度加入预测
            if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                double predictAngle = Math.atan2(dy, dx);
                targetAngle = normalizeAngle(targetAngle + predictAngle * predictScale);
            }
        }
        
        // 平滑转向
        double rotationSpeed = 0.1 + aggressiveness * 0.2;
        
        if (Math.abs(angleDiff) > 0.1) {
            if (angleDiff > 0 && angleDiff <= Math.PI) {
                this.angle += rotationSpeed;
            } else {
                this.angle -= rotationSpeed;
            }
            
            // 保持角度在合理范围
            this.angle = normalizeAngle(this.angle);
        } else {
            // 已经接近目标角度
            this.angle = targetAngle;
        }
        
        // 根据精度添加随机抖动
        if (precision < 0.9) {
            double jitter = (1.0 - precision) * 0.2 * (random.nextDouble() - 0.5);
            this.angle = normalizeAngle(this.angle + jitter);
        }
    }
    
    /**
     * 计算威胁等级
     */
    private double calculateThreatLevel(PlayerTank player) {
        double threat = 0;

        // 玩家距离越近威胁越大
        double distance = calculateDistance(player);
        threat += Math.max(0, 1 - distance / 300);

        // 玩家子弹越近威胁越大
        PlayerBullet nearestBullet = findNearestBullet(player);
        if (nearestBullet != null) {
            double bulletDistance = Math.sqrt(
                Math.pow(nearestBullet.getX() - x, 2) + 
                Math.pow(nearestBullet.getY() - y, 2)
            );
            
            if (bulletDistance < 150) {
                threat += (1 - bulletDistance / 150) * 2; // 子弹威胁权重更高
            }
        }

        // 考虑玩家是否在射击
        if (player.isShooting()) {
            threat += 0.3;
        }
        
        return Math.min(threat, 1.5); // 限制最大威胁级别
    }
    
    /**
     * 默认行为
     */
    private void normalBehavior(PlayerTank player, double distance, double levelFactor) {
        if (distance > 300) {
            moveToward(player, levelFactor);
        } else if (distance < 150) {
            evadeFrom(player, levelFactor);
        } else {
            strafingMove(player, levelFactor);
        }
    }
    
    /**
     * 攻击玩家行为
     */
    private void attackPlayer(PlayerTank player, double distance, double levelFactor) {
        if (distance > 200) {
            // 使用预测追击
            predictiveChase(player, levelFactor * 1.2);
        } else {
            // 近距离时，在战术距离保持并瞄准
            double angleDiff = Math.abs(normalizeAngle(angle - calculateAngleToPlayer(player)));
            if (angleDiff < 0.5) {
                // 已经瞄准玩家，保持距离
                maintainOptimalDistance(player, 150, 200, levelFactor);
            } else {
                // 未瞄准，先转向
                this.angle = calculateAngleToPlayer(player);
            }
        }
    }
    
    /**
     * 维持最佳作战距离
     */
    private void maintainOptimalDistance(PlayerTank player, double minDist, double maxDist, double levelFactor) {
        double distance = calculateDistance(player);
        
        if (distance < minDist) {
            // 太近，后退
            evadeFrom(player, levelFactor * 0.7);
        } else if (distance > maxDist) {
            // 太远，前进
            moveToward(player, levelFactor * 0.7);
        } else {
            // 合适距离，侧移
            strafingMove(player, levelFactor);
        }
    }
    
    /**
     * 战术移动
     */
    private void strategicMovement(PlayerTank player, double levelFactor) {
        double distance = calculateDistance(player);
        
        // 根据智能等级随机决策
        double rand = random.nextDouble();
        
        if (rand < intelligence) {
            // 智能决策 - 保持最佳射击距离
            maintainOptimalDistance(player, 150, 250, levelFactor);
        } else if (rand < intelligence + 0.3) {
            // 侧面移动，同时保持玩家在视野内
            strafingMove(player, levelFactor);
        } else {
            // 随机移动
            if (distance > 300) {
                moveToward(player, levelFactor * 0.8);
            } else {
                randomMove(levelFactor);
            }
        }
    }

    /**
     * 侧面移动
     */
    private void strafingMove(PlayerTank player, double levelFactor) {
        // 计算垂直于玩家方向的角度
        double angleToPlayer = calculateAngleToPlayer(player);
        double perpAngle;
        
        // 根据历史数据选择更优的侧移方向
        if (weights.getOrDefault("strafe_right", 0.5) > weights.getOrDefault("strafe_left", 0.5)) {
            perpAngle = angleToPlayer + Math.PI/2; // 向右侧移
        } else {
            perpAngle = angleToPlayer - Math.PI/2; // 向左侧移
        }
        
        // 计算新位置
        int newX = (int)(x + Math.cos(perpAngle) * currentSpeed * levelFactor);
        int newY = (int)(y + Math.sin(perpAngle) * currentSpeed * levelFactor);

        // 检查新位置是否有效
        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
            
            // 更新权重，记录这个方向侧移有效
            String strafeDir = perpAngle > angleToPlayer ? "strafe_right" : "strafe_left";
            weights.merge(strafeDir, 0.1, Double::sum);
        } else {
            // 如果碰撞，则换向
            perpAngle = angleToPlayer + (perpAngle > angleToPlayer ? -Math.PI/2 : Math.PI/2);
            newX = (int)(x + Math.cos(perpAngle) * currentSpeed * levelFactor);
            newY = (int)(y + Math.sin(perpAngle) * currentSpeed * levelFactor);
            
            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
                
                // 更新权重，记录方向切换
                String strafeDir = perpAngle > angleToPlayer ? "strafe_right" : "strafe_left";
                weights.merge(strafeDir, 0.1, Double::sum);
            }
        }
        
        // 保持面向玩家
        this.angle = angleToPlayer;
    }

    /**
     * 躲避子弹
     */
    private void evadeBullets(PlayerTank player, double levelFactor) {
        PlayerBullet nearestBullet = findNearestBullet(player);
        if (nearestBullet == null) return;
        
        double bulletDistance = Math.sqrt(
            Math.pow(nearestBullet.getX() - x, 2) +
            Math.pow(nearestBullet.getY() - y, 2)
        );

        // 增加躲避速度倍增器
        double evadeSpeedMultiplier = 2.0;
        if (bulletDistance < CRITICAL_BULLET_DISTANCE) {
            evadeSpeedMultiplier = 2.5; // 近距离时更快速躲避
        }

        // 计算子弹到坦克的向量
        double bulletAngle = Math.atan2(nearestBullet.getY() - y, nearestBullet.getX() - x);
        
        // 智能选择更好的躲避方向
        double evadeAngle = bulletAngle + selectBetterEvadeDirection(nearestBullet, player);

        // 快速移动到新位置
        int newX = (int)(x + Math.cos(evadeAngle) * currentSpeed * levelFactor * evadeSpeedMultiplier);
        int newY = (int)(y + Math.sin(evadeAngle) * currentSpeed * levelFactor * evadeSpeedMultiplier);

        // 检查新位置是否可用
        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
            
            // 智能AI不会立即转向躲避方向，仍然尝试面向玩家
            if (intelligence > 0.7) {
                // 面向玩家但略微偏向躲避方向
                double facingAngle = calculateAngleToPlayer(player);
                this.angle = normalizeAngle(facingAngle * 0.7 + evadeAngle * 0.3);
            } else {
                // 低智能时直接面向躲避方向
                this.angle = evadeAngle;
            }
        } else {
            // 如果不能移动到新位置，尝试反向躲避
            evadeAngle = bulletAngle - selectBetterEvadeDirection(nearestBullet, player);
            newX = (int)(x + Math.cos(evadeAngle) * currentSpeed * levelFactor * evadeSpeedMultiplier);
            newY = (int)(y + Math.sin(evadeAngle) * currentSpeed * levelFactor * evadeSpeedMultiplier);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
                
                if (intelligence > 0.7) {
                    double facingAngle = calculateAngleToPlayer(player);
                    this.angle = normalizeAngle(facingAngle * 0.7 + evadeAngle * 0.3);
                } else {
                    this.angle = evadeAngle;
                }
            }
        }
    }
    
    /**
     * 选择更好的躲避方向
     */
    private double selectBetterEvadeDirection(PlayerBullet bullet, PlayerTank player) {
        double leftAngle = Math.PI/2;
        double rightAngle = -Math.PI/2;

        // 计算向左和向右躲避的新位置
        double leftX = x + Math.cos(angle + leftAngle) * 50;
        double leftY = y + Math.sin(angle + leftAngle) * 50;
        double rightX = x + Math.cos(angle + rightAngle) * 50;
        double rightY = y + Math.sin(angle + rightAngle) * 50;

        // 选择远离玩家的方向
        double leftDistToPlayer = Math.sqrt(Math.pow(leftX - player.getX(), 2) + Math.pow(leftY - player.getY(), 2));
        double rightDistToPlayer = Math.sqrt(Math.pow(rightX - player.getX(), 2) + Math.pow(rightY - player.getY(), 2));

        return leftDistToPlayer > rightDistToPlayer ? leftAngle : rightAngle;
    }

    /**
     * 判断是否需要躲避子弹
     */
    private boolean shouldEvadeBullets(PlayerTank player) {
        if (!player.isShooting()) return false;

        // 使用缓存的最近子弹优化性能
        if (cachedNearestBullet == null) {
            cachedNearestBullet = findNearestBullet(player);
            lastBulletCheck = System.currentTimeMillis();
        }
        
        if (cachedNearestBullet != null && cachedNearestBullet.isActive()) {
            double bulletDistance = Math.sqrt(
                Math.pow(cachedNearestBullet.getX() - x, 2) + 
                Math.pow(cachedNearestBullet.getY() - y, 2)
            );

            if (bulletDistance < BULLET_DETECT_DISTANCE) {
                // 计算子弹方向与AI坦克的夹角
                double bulletAngle = Math.atan2(
                    cachedNearestBullet.getY() - y, 
                    cachedNearestBullet.getX() - x
                );
                double angleDiff = Math.abs(normalizeAngle(angle - bulletAngle));

                // 根据距离和角度判断威胁等级
                if (bulletDistance < CRITICAL_BULLET_DISTANCE) {
                    return true; // 跴离过近立即躲避
                }

                if (bulletDistance < IMMEDIATE_EVADE_DISTANCE && angleDiff < Math.PI/3) {
                    return true; // 中等距离且朝向较准确时躲避
                }

                // 远距离但朝向非常准确时也躲避
                if (angleDiff < Math.PI/4) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 预测性追击
     */
    private void predictiveChase(PlayerTank player, double speedFactor) {
        // 智能等级影响预测质量
        double predictDistance = 50 * speedFactor * intelligence;
        
        // 基于玩家历史位置预测移动方向
        double dx = player.getX() - lastPlayerX;
        double dy = player.getY() - lastPlayerY;
        
        // 计算预测位置
        double predictAngle = (dx == 0 && dy == 0) ? 
                            player.getAngle() : // 如果玩家静止，使用其朝向
                            Math.atan2(dy, dx); // 否则使用移动方向
                            
        double predictX = player.getX() + Math.cos(predictAngle) * predictDistance;
        double predictY = player.getY() + Math.sin(predictAngle) * predictDistance;

        // 向预测位置移动
        double moveX = predictX - x;
        double moveY = predictY - y;
        double moveDistance = Math.sqrt(moveX * moveX + moveY * moveY);

        if (moveDistance > 0) {
            // 更新朝向角度
            double moveAngle = Math.atan2(moveY, moveX);
            this.angle = moveAngle;
            
            // 计算新位置
            int newX = (int)(x + (moveX/moveDistance) * currentSpeed * speedFactor);
            int newY = (int)(y + (moveY/moveDistance) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }

    /**
     * 预测射击 - 考虑目标移动
     */
    private boolean shouldShoot(PlayerTank player, double distance, double angleToPlayer, double levelFactor) {
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < FIRE_INTERVAL) {
            return false;
        }
        
        // 基础射击概率随关卡递增
        double baseProb = 0.3 * levelFactor;
        
        // 最佳射击距离范围
        double optimalDistance = 200;
        double distanceFactor = 1 - Math.min(1, Math.abs(distance - optimalDistance) / 300);
        
        // 计算角度差
        double angleDiff = Math.abs(normalizeAngle(angle - angleToPlayer));
        
        // 精度因子 - 角度差越小，命中概率越高
        double angleFactor = Math.max(0, 1 - (angleDiff / (Math.PI/2)));
        
        // 提前量预测 - 根据目标移动速度调整射击时机
        if (lastPlayerX != 0 && lastPlayerY != 0) {
            double dx = player.getX() - lastPlayerX;
            double dy = player.getY() - lastPlayerY;
            
            if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                // 目标正在移动 - 预测射击
                double moveSpeed = Math.sqrt(dx*dx + dy*dy);
                double predictTime = distance / (double)EnemyBullet.DEFAULT_SPEED;
                
                // 智能AI会预测玩家位置
                if (intelligence > 0.6) {
                    double predictX = player.getX() + dx * predictTime * intelligence;
                    double predictY = player.getY() + dy * predictTime * intelligence;
                    
                    // 计算预测角度
                    double predictAngle = Math.atan2(predictY - y, predictX - x);
                    
                    // 如果预测角度与当前角度差异不大，提高射击概率
                    double predictAngleDiff = Math.abs(normalizeAngle(angle - predictAngle));
                    if (predictAngleDiff < Math.PI/8) {
                        angleFactor *= 1.5;
                    }
                }
            }
        }
        
        // 综合射击概率计算
        double shootProb = baseProb * 
                         (0.3 + 0.3 * distanceFactor + 0.4 * angleFactor) *
                         (0.5 + precision * 0.5) *
                         (0.5 + aggressiveness * 0.5);
        
        // 额外因素调整
        if (player.getHealth() < 30) {
            shootProb *= 1.3; // 玩家低生命值时更积极射击
        }
        
        if (health < 0.3) {
            shootProb *= 0.7; // 自身低生命值时减少射击频率，保持隐蔽
        }
        
        return random.nextDouble() < shootProb;
    }

    /**
     * 标准化角度
     */
    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }

    /**
     * 更新子弹位置
     */
    public void updateBullets() {
        // 移除失效的子弹
        bullets.removeIf(bullet -> !bullet.isActive());

        // 更新剩余子弹的位置
        for (EnemyBullet bullet : bullets) {
            bullet.updatePosition();
        }
    }
    
    /**
     * 死亡时的学习加强
     */
    public void onDeath(PlayerTank player) {
        // 死亡时进行强化学习
        double deathPenalty = -0.5; // 死亡惩罚
        learn(false, player);
        
        // 额外更新权重
        updateBasicWeights(deathPenalty * 2);
        updatePositionBasedLearning(player, deathPenalty);
        updatePatternRecognition(deathPenalty);
        
        // 动态调整性格特性
        adjustPersonality(false);
        
        // 保存学习数据
        saveLearnedData();
    }
    
    /**
     * 调整AI性格特性
     */
    private void adjustPersonality(boolean success) {
        // 根据成功/失败调整性格
        double adjustment = success ? 0.05 : -0.05;
        
        // 随机选择性格特性调整
        switch (random.nextInt(3)) {
            case 0:
                aggressiveness = Math.max(0.1, Math.min(1.0, aggressiveness + adjustment));
                weights.put("personality_aggressive", aggressiveness);
                break;
            case 1:
                intelligence = Math.max(0.1, Math.min(1.0, intelligence + adjustment));
                weights.put("personality_intelligence", intelligence);
                break;
            case 2:
                precision = Math.max(0.1, Math.min(1.0, precision + adjustment));
                weights.put("personality_precision", precision);
                break;
        }
    }

    /**
     * 计算到玩家的角度
     */
    private double calculateAngleToPlayer(PlayerTank player) {
        if (player == null) return 0;
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        return Math.atan2(dy, dx);
    }

    /**
     * 计算到玩家的距离
     */
    private double calculateDistance(PlayerTank player) {
        if (player == null) return 0;
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 向玩家移动
     */
    private void moveToward(PlayerTank player, double speedFactor) {
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // 更新角度
            this.angle = Math.atan2(dy, dx);

            // 使用当前角度计算新位置
            int newX = (int)(x + Math.cos(this.angle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(this.angle) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }

    /**
     * 躲避玩家
     */
    private void evadeFrom(PlayerTank player, double speedFactor) {
        // 优先躲避子弹
        PlayerBullet nearestBullet = findNearestBullet(player);
        if (nearestBullet != null) {
            double bulletAngle = Math.atan2(nearestBullet.getY() - y, nearestBullet.getX() - x);
            // 更新角度
            this.angle = bulletAngle + (Math.random() > 0.5 ? Math.PI/2 : -Math.PI/2);

            // 使用更新后的角度移动
            int newX = (int)(x + Math.cos(this.angle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(this.angle) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
            return;
        }

        // 躲避玩家
        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // 更新角度
            this.angle = Math.atan2(dy, dx);

            // 使用更新后的角度移动
            int newX = (int)(x + (Math.cos(this.angle) * currentSpeed * speedFactor));
            int newY = (int)(y + (Math.sin(this.angle) * currentSpeed * speedFactor));

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }
    
    /**
     * 寻找最近的子弹
     */
    private PlayerBullet findNearestBullet(PlayerTank player) {
        if (cachedNearestBullet != null && 
            System.currentTimeMillis() - lastBulletCheck < 100 &&
            cachedNearestBullet.isActive()) {
            return cachedNearestBullet;
        }
        
        PlayerBullet nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (PlayerBullet bullet : player.getBullets()) {
            if (!bullet.isActive()) continue;
            
            double distance = Math.sqrt(
                Math.pow(bullet.getX() - x, 2) + 
                Math.pow(bullet.getY() - y, 2)
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = bullet;
            }
        }

        cachedNearestBullet = nearest;
        lastBulletCheck = System.currentTimeMillis();
        return nearest;
    }

    /**
     * 随机移动
     */
    private void randomMove(double speedFactor) {
        double moveAngle = Math.random() * 2 * Math.PI;
        int newX = (int)(x + Math.cos(moveAngle) * currentSpeed * speedFactor);
        int newY = (int)(y + Math.sin(moveAngle) * currentSpeed * speedFactor);

        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
            // 保持原角度，不改变朝向
        }
    }

    /**
     * 学习方法
     */
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
        
        // 调整性格特性
        if (random.nextDouble() < 0.2) { // 20%概率调整性格
            adjustPersonality(success);
        }

        normalizeWeights();
    }
    
    /**
     * 更新基础权重
     */
    private void updateBasicWeights(double adjustment) {
        weights.merge("chase", adjustment, (oldVal, newVal) ->
                oldVal * decayFactor + newVal * (1 - decayFactor));
        weights.merge("evade", adjustment, (oldVal, newVal) ->
                oldVal * decayFactor + newVal * (1 - decayFactor));
        weights.merge("shoot", adjustment, (oldVal, newVal) ->
                oldVal * decayFactor + newVal * (1 - decayFactor));
    }

    /**
     * 更新基于位置的学习
     */
    private void updatePositionBasedLearning(PlayerTank player, double adjustment) {
        // 根据相对位置学习
        double relX = player.getX() - this.x;
        double relY = player.getY() - this.y;
        String posKey = "pos_" + (relX > 0 ? "R" : "L") + (relY > 0 ? "D" : "U");
        weights.merge(posKey, adjustment, Double::sum);
    }

    /**
     * 更新模式识别
     */
    private void updatePatternRecognition(double adjustment) {
        // 更新玩家模式识别权重
        playerPatterns.forEach((key, value) ->
                weights.merge("pattern_" + key, value * adjustment, Double::sum));
    }

    /**
     * 标准化权重
     */
    private void normalizeWeights() {
        double total = weights.values().stream()
                .filter(v -> v > 0) // 只考虑正值
                .mapToDouble(Double::doubleValue)
                .sum();
                
        if (total == 0) return; // 防止除以零

        // 只标准化正值，负值保持原样
        weights.replaceAll((k, v) -> v > 0 ? v / total : v);
    }

    /**
     * 保存学习数据
     */
    public void saveLearnedData() {
        createDataDirectory(); // 确保目录存在
        
        File saveFile = new File(DATA_DIR, SAVE_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(saveFile))) {
                
            // 保存完整的学习数据
            Map<String, Object> saveData = new HashMap<>();
            saveData.put("weights", weights);
            saveData.put("playerPatterns", playerPatterns);
            saveData.put("personality", new double[]{aggressiveness, intelligence, precision});
            saveData.put("level", ConfigTool.getLevel());
            saveData.put("timestamp", System.currentTimeMillis());
            
            oos.writeObject(saveData);
            System.out.println("AI数据已保存到: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存AI数据失败: " + e.getMessage());
        }
    }

    /**
     * 加载学习数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> loadLearnedData() {
        File file = new File(DATA_DIR, SAVE_FILE);
        if (!file.exists()) {
            System.out.println("未找到AI数据文件，创建默认权重");
            return createDefaultWeights();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file))) {
            
            Map<String, Object> saveData = (Map<String, Object>) ois.readObject();

            // 加载性格特性
            if (saveData.containsKey("personality")) {
                double[] personality = (double[]) saveData.get("personality");
                if (personality.length >= 3) {
                    this.aggressiveness = personality[0];
                    this.intelligence = personality[1];
                    this.precision = personality[2];
                }
            }
            
            // 加载玩家模式数据
            if (saveData.containsKey("playerPatterns")) {
                this.playerPatterns = (Map<String, Double>) saveData.get("playerPatterns");
            }
            
            // 加载权重
            if (saveData.containsKey("weights")) {
                return (Map<String, Double>) saveData.get("weights");
            } else {
                // 为兼容旧版数据，创建一个新的Map并填充Double类型的值
                Map<String, Double> result = new HashMap<>();
                for (Map.Entry<String, Object> entry : saveData.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        result.put(entry.getKey(), ((Number)entry.getValue()).doubleValue());
                    }
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("加载AI数据失败: " + e.getMessage());
        }
        return createDefaultWeights();
    }

    /**
     * 创建默认权重
     */
    private Map<String, Double> createDefaultWeights() {
        Map<String, Double> defaults = new HashMap<>();
        
        // 基础行为权重
        defaults.put("chase", 0.9);     // 追击倾向
        defaults.put("evade", 0.4);     // 躲避倾向
        defaults.put("shoot", 0.85);    // 射击倾向
        
        // 策略权重
        defaults.put("aggressive", 0.9); // 激进程度
        defaults.put("defensive", 0.3);  // 防御程度
        defaults.put("pattern_recognition", 0.6); // 模式识别
        defaults.put("predict", 0.4);    // 预测能力
        
        // 侧移权重
        defaults.put("strafe_left", 0.5);
        defaults.put("strafe_right", 0.5);
        
        // 性格特性
        defaults.put("personality_aggressive", aggressiveness);
        defaults.put("personality_intelligence", intelligence);
        defaults.put("personality_precision", precision);
        
        return defaults;
    }

    @Override
    public void fire() {

    }

    /**
     * 发射子弹 - 考虑弹道扩散
     */
    @Override
    public void fire(PlayerTank player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= FIRE_INTERVAL) {
            // 炮管位置计算
            int barrelLength = width / 2 + 5;
            int bulletX = (int) (x + width / 2 + Math.cos(angle) * barrelLength);
            int bulletY = (int) (y + height / 2 + Math.sin(angle) * barrelLength);

            // 精度影响弹道扩散
            double spreadFactor = (1.0 - precision) * 0.2;
            double randomSpread = (random.nextDouble() - 0.5) * spreadFactor;
            
            // 智能预测
            double predictFactor = 0;
            if (intelligence > 0.5 && lastPlayerX != 0 && lastPlayerY != 0 && player != null) {
                // 计算玩家移动向量
                double dx = player.getX() - lastPlayerX;
                double dy = player.getY() - lastPlayerY;
                
                // 计算预测角度
                double playerMoveAngle = Math.atan2(dy, dx);
                double playerSpeed = Math.sqrt(dx*dx + dy*dy);
                
                // 预测补偿 - 越聪明的AI补偿越准确
                predictFactor = (playerSpeed / 20.0) * (intelligence - 0.5);
                // 限制补偿最大值
                predictFactor = Math.min(0.15, predictFactor);
                
                // 角度差决定补偿方向
                double angleDiff = normalizeAngle(playerMoveAngle - angle);
                if (Math.abs(angleDiff) > Math.PI/2) {
                    predictFactor = -predictFactor;
                }
            }
            
            // 最终射击角度
            double fireAngle = angle + randomSpread + predictFactor;
            
            // 创建子弹
            EnemyBullet bullet = new EnemyBullet(bulletX, bulletY, fireAngle);
            
            // 添加到列表并更新时间
            bullets.add(bullet);
            lastActionTime = currentTime;
        }
    }

    @Override
    public void useSkill() {
        // 技能实现（可扩展）
    }

    @Override
    public int getDirection() {
        // 根据角度返回方向
        double normAngle = normalizeAngle(angle);
        if (normAngle <= Math.PI/4 || normAngle > 7*Math.PI/4) return 1; // 右
        if (normAngle > Math.PI/4 && normAngle <= 3*Math.PI/4) return 2; // 下
        if (normAngle > 3*Math.PI/4 && normAngle <= 5*Math.PI/4) return 3; // 左
        return 0; // 上
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
    
    /**
     * 根据玩家表现动态调整AI难度
     */
    public void dynamicDifficultyAdjustment(int playerScore, int aiScore) {
        // 计算难度调整因子
        double scoreDiff = playerScore - aiScore;
        double adjustmentFactor = Math.min(0.2, Math.abs(scoreDiff) * 0.05);
        
        if (scoreDiff > 0) {
            // 玩家领先 - 增加AI能力
            aggressiveness = Math.min(1.0, aggressiveness + adjustmentFactor);
            intelligence = Math.min(1.0, intelligence + adjustmentFactor);
            precision = Math.min(1.0, precision + adjustmentFactor);
        } else if (scoreDiff < 0) {
            // AI领先 - 降低AI能力
            aggressiveness = Math.max(0.3, aggressiveness - adjustmentFactor);
            intelligence = Math.max(0.3, intelligence - adjustmentFactor);
            precision = Math.max(0.3, precision - adjustmentFactor);
        }
        
        // 更新权重
        weights.put("personality_aggressive", aggressiveness);
        weights.put("personality_intelligence", intelligence);
        weights.put("personality_precision", precision);
    }
    
    /**
     * 检查碰撞并考虑边界
     */
    public boolean checkCollision(int newX, int newY) {
        // 先检查边界
        if (newX < 0 || newY < 0 || 
            newX + width > 800 || newY + height > 600) {
            return false;
        }
        
        // 添加安全检查
        if (detector == null) {
            System.out.println("警告: 碰撞检测器为null，使用默认安全碰撞检测");
            // 默认行为：只检查边界，不检查其他碰撞
            return true;
        }
        
        // 使用碰撞检测器
        return !detector.isColliding(newX, newY, width, height);
    }

    public void draw(Graphics g) {
        if (!isAlive() || tankImage == null) return;
        
        Graphics2D g2d = (Graphics2D) g.create();
        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 坦克中心点
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        
        // 变换坐标系到坦克中心
        g2d.translate(centerX, centerY);
        g2d.rotate(angle);
        
        // 绘制坦克（从中心点偏移半个宽高）
        g2d.drawImage(tankImage, -width / 2, -height / 2, width, height, null);
        
        // 绘制状态指示器（仅在调试模式下）
        if (ConfigTool.isDebugMode()) {
            drawDebugInfo(g2d);
        }
        
        g2d.dispose();
    }

    private void drawDebugInfo(Graphics2D g2d) {
        // 绘制不同行为状态的视觉指示
        switch (currentBehaviorState) {
            case BEHAVIOR_ATTACKING:
                g2d.setColor(new Color(255, 0, 0, 90));
                g2d.fillOval(-20, -20, 40, 40);
                break;
            case BEHAVIOR_EVADING:
                g2d.setColor(new Color(0, 0, 255, 90));
                g2d.fillOval(-20, -20, 40, 40);
                break;
            case BEHAVIOR_STRATEGIC:
                g2d.setColor(new Color(0, 255, 0, 90));
                g2d.fillOval(-20, -20, 40, 40);
                break;
        }
        
        // 绘制健康状态
        int healthBarWidth = 40;
        int healthBarHeight = 5;
        g2d.setColor(Color.BLACK);
        g2d.drawRect(-healthBarWidth/2, -height/2 - 10, healthBarWidth, healthBarHeight);
        g2d.setColor(new Color(255, (int)(health * 255), 0));
        g2d.fillRect(-healthBarWidth/2, -height/2 - 10, (int)(healthBarWidth * health), healthBarHeight);
    }

    /**
     * 调试模式 - 显示AI信息
     */
    public String getAIDebugInfo() {
        if (!ConfigTool.isDebugMode()) return "";
        
        StringBuilder info = new StringBuilder();
        info.append("AI状态: ").append(getBehaviorName()).append("\n");
        info.append("生命值: ").append(health).append("\n");
        info.append("攻击性: ").append(String.format("%.2f", aggressiveness)).append("\n");
        info.append("智能: ").append(String.format("%.2f", intelligence)).append("\n");
        info.append("精度: ").append(String.format("%.2f", precision)).append("\n");
        
        return info.toString();
    }

    private String getBehaviorName() {
        switch(currentBehaviorState) {
            case BEHAVIOR_ATTACKING: return "攻击";
            case BEHAVIOR_EVADING: return "躲避";
            case BEHAVIOR_STRATEGIC: return "策略";
            default: return "正常";
        }
    }


    /**
     * 设置碰撞检测器
     */
    public void setDetector(CollisionDetector detector) {
        this.detector = detector;
    }
}