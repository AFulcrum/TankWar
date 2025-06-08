package Config;

import InterFace.CollisionDetector;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class AITank extends AbstractTank {
    // 图像和方向
    private double angle = 0; // 0表示向右，逆时针为正（数学坐标系）
    private final String tankPath = "/Images/TankImage/EnemyTank/tankR.gif";
    private Image tankImage;
    
    // 移动和碰撞
    private CollisionDetector detector;
    private int currentSpeed = 2;
    private static final int BASE_SPEED = 2;
    
    // 子弹管理
    private List<EnemyBullet> bullets;
    private static int fireInterval = 2500; // Changed from final to allow modification
    
    // 时间控制
    private long lastActionTime = 0;
    private long lastFireTime = 0;
    private long lastPlayerPositionUpdate = 0;
    private long lastStateChangeTime = 0;
    private static long actionDelay = 100; // Changed from final to allow modification
    private static final long PATTERN_UPDATE_INTERVAL = 1000;
    private static final long STATE_CHANGE_COOLDOWN = 1500; // 冷却时间

    public void setAlive(boolean b) {
        super.setAlive(b);
        if (!b) {
            // AI坦克死亡时清空子弹
            bullets.clear();
        }
    }

    // AI行为状态
    private enum BehaviorState {
        NORMAL,      // 普通行为
        ATTACKING,   // 攻击模式
        EVADING,     // 躲避模式
        STRATEGIC    // 策略模式
    }
    private BehaviorState currentBehaviorState = BehaviorState.NORMAL;
    private BehaviorState previousState = BehaviorState.NORMAL;
    
    // AI性格特性
    private double aggressiveness = 0.7;  // 攻击性 (0-1)
    private double intelligence = 0.6;   // 智能程度 (0-1)
    private double precision = 0.8;      // 精准度 (0-1)
    
    // 学习系统
    private double learningRate = 0.05;
    private double decayFactor = 0.95;
    private Map<String, Double> weights = new HashMap<>();
    private Map<String, Double> playerPatterns = new HashMap<>();
    
    // 生涯统计
    private int lifetimeShots = 0;
    private int lifetimeHits = 0;
    private int matchesPlayed = 0;
    
    // 文件路径
    private static final String DATA_DIR = System.getProperty("user.dir") + File.separator + "Data";
    private static final String SAVE_FILE = "AITankData.dat";
    
    // 性能优化
    private final Random random = new Random();
    private int lastPlayerX = 0;
    private int lastPlayerY = 0;
    private double predictFactor = 0.0; // 子弹预测因子
    
    // 威胁检测距离
    private static final double BULLET_DETECT_DISTANCE = 250;
    private static final double IMMEDIATE_EVADE_DISTANCE = 150;
    
    /**
     * AI坦克构造函数
     */
    public AITank(int x, int y, CollisionDetector detector) {
        super(x, y, 64, 64, 1, detector);
        this.detector = detector;
        
        // 初始化数据和组件
        createDataDirectory();
        loadLearnedData();
        initializePersonality();
        loadTankImage();
        
        this.bullets = new ArrayList<>();
        this.lastActionTime = System.currentTimeMillis();
        this.lastPlayerPositionUpdate = System.currentTimeMillis();
        this.lastStateChangeTime = System.currentTimeMillis();
        
        // 增加射击权重初始值
        if (!weights.containsKey("shoot")) {
            weights.put("shoot", 0.9);
        }
    }
    
    /**
     * 加载坦克图像
     */
    private void loadTankImage() {
        try {
            URL url = getClass().getResource(tankPath);
            if (url == null) {
                System.err.println("AI坦克图片不存在: " + tankPath);
                return;
            }
            ImageIcon icon = new ImageIcon(url);
            tankImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            
            // 不需要额外旋转，因为tankR.gif已经是向右朝向
        } catch (Exception e) {
            System.err.println("无法加载AI坦克图像: " + e.getMessage());
        }
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
     * 更新AI状态和行为
     */
    public void updateAI(PlayerTank player, int currentLevel) {
        if (!isAlive() || player == null || !player.isAlive()) return;

        // 适应当前难度级别
        adaptToDifficulty(currentLevel);
        
        long currentTime = System.currentTimeMillis();
        
        // 更新玩家模式学习数据
        if (currentTime - lastPlayerPositionUpdate > 200) {
            updatePlayerPatterns(player);
            lastPlayerPositionUpdate = currentTime;
        }

        // 控制AI行为更新频率
        long dynamicDelay = (long)(actionDelay * (1 - currentLevel * 0.03));
        if (currentTime - lastActionTime < dynamicDelay) {
            // 即使不执行主要行为更新，也要更新子弹
            updateBullets();
            return;
        }

        // 计算基本状态
        double distance = calculateDistance(player);
        double angleToPlayer = calculateAngleToPlayer(player);
        double threatLevel = calculateThreatLevel(player);
        
        // 根据威胁级别调整速度
        currentSpeed = Math.min(BASE_SPEED * 2, (int)(BASE_SPEED * (1 + threatLevel * 0.5)));

        // 决定行为状态
        decideBehaviorState(player, distance, threatLevel);
        
        // 平滑转向玩家
        updateFacingAngle(angleToPlayer, player);
        
        // 根据状态执行行为
        double levelFactor = 1 + (currentLevel * 0.15);
        executeBehavior(player, distance, levelFactor);

        // 智能射击决策
        if (shouldShoot(player, distance, angleToPlayer, levelFactor)) {
            fire(player);
        }

        updateBullets();
        lastActionTime = currentTime;
        
        // 随机学习 - 每500次更新学习一次(约10秒)
        if (random.nextInt(500) == 0) {
            boolean success = isPerformingWell(player);
            advancedLearn(success, player);
        }
    }
    
    /**
     * 决定AI的行为状态
     */
    private void decideBehaviorState(PlayerTank player, double distance, double threatLevel) {
        // 添加状态持续时间跟踪
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStateChangeTime < STATE_CHANGE_COOLDOWN) {
            // 在冷却期内不改变状态，减少抽搐
            return;
        }
        
        // 检查子弹威胁
        if (shouldEvadeBullets(player)) {
            currentBehaviorState = BehaviorState.EVADING;
            return;
        }
        
        // 低生命值时更保守
        if (health < 0.4) {
            if (random.nextDouble() < 0.7) {
                currentBehaviorState = BehaviorState.EVADING;
                return;
            }
        }
        
        // 基于距离和威胁的决策
        if (distance < 150) {
            // 近距离策略
            if (threatLevel > 0.7) {
                double aggressiveThreshold = 0.4 + (health * 0.3);
                currentBehaviorState = random.nextDouble() < aggressiveness * aggressiveThreshold ? 
                                      BehaviorState.ATTACKING : BehaviorState.EVADING;
            } else {
                currentBehaviorState = BehaviorState.ATTACKING;
            }
        } else if (distance > 400) {
            // 远距离时追击
            currentBehaviorState = BehaviorState.ATTACKING;
        } else {
            // 中等距离时
            if (random.nextDouble() < intelligence) {
                currentBehaviorState = BehaviorState.STRATEGIC;
            } else {
                boolean hasAdvantage = health > player.getHealth() / 100.0;
                currentBehaviorState = hasAdvantage ? BehaviorState.ATTACKING : BehaviorState.STRATEGIC;
            }
        }
        
        // 基于学习权重微调决策
        adjustBehaviorBasedOnLearning();
        
        // 记录状态变化时间
        if (previousState != currentBehaviorState) {
            previousState = currentBehaviorState;
            lastStateChangeTime = currentTime;
        }
    }
    
    /**
     * 基于学习权重调整行为决策
     */
    private void adjustBehaviorBasedOnLearning() {
        double attackWeight = weights.getOrDefault("chase", 0.5);
        double evadeWeight = weights.getOrDefault("evade", 0.5);
        double strategicWeight = weights.getOrDefault("strategic", 0.5);
        
        double totalWeight = attackWeight + evadeWeight + strategicWeight;
        double attackProb = attackWeight / totalWeight;
        double evadeProb = evadeWeight / totalWeight;
        
        // 有10%概率根据学习权重重新选择行为
        if (random.nextDouble() < 0.1) {
            double rand = random.nextDouble();
            if (rand < attackProb) {
                currentBehaviorState = BehaviorState.ATTACKING;
            } else if (rand < attackProb + evadeProb) {
                currentBehaviorState = BehaviorState.EVADING;
            } else {
                currentBehaviorState = BehaviorState.STRATEGIC;
            }
        }
    }
    
    /**
     * 执行当前行为状态的行动
     */
    private void executeBehavior(PlayerTank player, double distance, double levelFactor) {
        switch (currentBehaviorState) {
            case NORMAL:
                normalBehavior(player, distance, levelFactor);
                break;
            case ATTACKING:
                attackPlayer(player, distance, levelFactor);
                break;
            case EVADING:
                evadeBullets(player, levelFactor);
                break;
            case STRATEGIC:
                strategicMovement(player, levelFactor);
                break;
        }
    }
    
    /**
     * 普通行为模式
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
            // 远距离使用预测追击
            predictiveChase(player, levelFactor * 1.2);
        } else {
            // 近距离时，保持战术距离
            maintainOptimalDistance(player, 150, 200, levelFactor);
        }
    }
    
    /**
     * 保持最佳射击距离
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
     * 策略性移动
     */
    private void strategicMovement(PlayerTank player, double levelFactor) {
        double distance = calculateDistance(player);
        double rand = random.nextDouble();
        
        if (rand < intelligence) {
            // 智能决策 - 保持最佳射击距离
            maintainOptimalDistance(player, 150, 250, levelFactor);
        } else if (rand < intelligence + 0.3) {
            // 侧面移动
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
        
        // 根据权重选择侧移方向
        if (weights.getOrDefault("strafe_right", 0.5) > weights.getOrDefault("strafe_left", 0.5)) {
            perpAngle = angleToPlayer + Math.PI/2; // 向右侧移
        } else {
            perpAngle = angleToPlayer - Math.PI/2; // 向左侧移
        }
        
        // 计算新位置
        int newX = (int)(x + Math.cos(perpAngle) * currentSpeed * levelFactor);
        int newY = (int)(y + Math.sin(perpAngle) * currentSpeed * levelFactor);

        // 检查新位置
        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
            
            // 更新权重
            String strafeDir = perpAngle > angleToPlayer ? "strafe_right" : "strafe_left";
            weights.merge(strafeDir, 0.1, Double::sum);
        } else {
            // 碰撞，换向
            perpAngle = angleToPlayer + (perpAngle > angleToPlayer ? -Math.PI/2 : Math.PI/2);
            newX = (int)(x + Math.cos(perpAngle) * currentSpeed * levelFactor);
            newY = (int)(y + Math.sin(perpAngle) * currentSpeed * levelFactor);
            
            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
                
                String strafeDir = perpAngle > angleToPlayer ? "strafe_right" : "strafe_left";
                weights.merge(strafeDir, 0.1, Double::sum);
            }
        }
        
        // 保持面向玩家
        this.angle = angleToPlayer;
    }
    
    /**
     * 躲避玩家子弹
     */
    private void evadeBullets(PlayerTank player, double levelFactor) {
        PlayerBullet nearestBullet = findNearestBullet(player);
        if (nearestBullet == null) return;
        
        double bulletDistance = Math.sqrt(
            Math.pow(nearestBullet.getX() - x, 2) +
            Math.pow(nearestBullet.getY() - y, 2)
        );

        // 增加躲避速度
        double evadeSpeedMultiplier = 1.5;
        if (bulletDistance < 50) {
            evadeSpeedMultiplier = 2.0; // 近距离时更快速躲避
        }

        // 计算子弹到坦克的向量
        double bulletAngle = Math.atan2(nearestBullet.getY() - y, nearestBullet.getX() - x);
        
        // 选择垂直于子弹方向的躲避方向
        double evadeAngle = bulletAngle + Math.PI/2;
        if (random.nextBoolean()) {
            evadeAngle = bulletAngle - Math.PI/2;
        }

        // 移动到新位置
        int newX = (int)(x + Math.cos(evadeAngle) * currentSpeed * levelFactor * evadeSpeedMultiplier);
        int newY = (int)(y + Math.sin(evadeAngle) * currentSpeed * levelFactor * evadeSpeedMultiplier);

        if (checkCollision(newX, newY)) {
            x = newX;
            y = newY;
            
            // 智能AI不会立即转向躲避方向
            if (intelligence > 0.7) {
                // 面向玩家但略微偏向躲避方向
                double facingAngle = calculateAngleToPlayer(player);
                this.angle = normalizeAngle(facingAngle * 0.7 + evadeAngle * 0.3);
            } else {
                this.angle = evadeAngle;
            }
        } else {
            // 反向躲避
            evadeAngle = bulletAngle + (evadeAngle > bulletAngle ? -Math.PI : Math.PI);
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
     * 是否需要躲避子弹
     */
    private boolean shouldEvadeBullets(PlayerTank player) {
        if (!player.isShooting()) return false;

        PlayerBullet nearestBullet = findNearestBullet(player);
        if (nearestBullet != null && nearestBullet.isActive()) {
            double bulletDistance = Math.sqrt(
                Math.pow(nearestBullet.getX() - x, 2) + 
                Math.pow(nearestBullet.getY() - y, 2)
            );

            if (bulletDistance < BULLET_DETECT_DISTANCE) {
                // 计算子弹方向与AI坦克的夹角
                double bulletAngle = Math.atan2(
                    nearestBullet.getY() - y, 
                    nearestBullet.getX() - x
                );
                double angleDiff = Math.abs(normalizeAngle(angle - bulletAngle));

                // 根据距离和角度判断威胁
                if (bulletDistance < 50) {
                    return true; // 距离过近立即躲避
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
     * 找到最近的玩家子弹
     */
    private PlayerBullet findNearestBullet(PlayerTank player) {
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

        return nearest;
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
     * 向玩家移动
     */
    private void moveToward(PlayerTank player, double speedFactor) {
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // 计算角度 - 使用数学坐标系（0度为右）
            this.angle = Math.atan2(dy, dx);
            
            // 直接使用标准三角函数计算
            int newX = (int)(x + Math.cos(this.angle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(this.angle) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
    }
    
    /**
     * 远离玩家移动
     */
    private void evadeFrom(PlayerTank player, double speedFactor) {
        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // 更新角度
            this.angle = Math.atan2(dy, dx);

            // 使用当前角度移动
            int newX = (int)(x + Math.cos(this.angle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(this.angle) * currentSpeed * speedFactor);

            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }
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
            // 不改变朝向
        }
    }
    
    /**
     * 平滑转向目标角度
     */
    private void updateFacingAngle(double targetAngle, PlayerTank player) {
        // 计算角度差
        double angleDiff = normalizeAngle(targetAngle - this.angle);
        
        // 设置最小角度变化阈值，避免微小角度变化导致抽搐
        double minAngleChange = 0.02;
        
        // 如果角度差非常小，则直接设置为目标角度，避免抽搐
        if (Math.abs(angleDiff) < minAngleChange) {
            this.angle = targetAngle;
            return;
        }
        
        // 智能预测移动方向
        if (intelligence > 0.5 && player.isMoving()) {
            double predictScale = 0.2 + intelligence * 0.3;
            double dx = player.getX() - lastPlayerX;
            double dy = player.getY() - lastPlayerY;
            
            if (Math.abs(dx) > 2 || Math.abs(dy) > 2) { // 增加阈值避免微小移动导致角度变化
                // 计算玩家移动角度和速度
                double playerMoveAngle = Math.atan2(dy, dx);
                double playerSpeed = Math.sqrt(dx*dx + dy*dy);
                
                // 限制预测补偿，避免过度转向
                predictScale = Math.min(0.1, (playerSpeed / 20.0) * (intelligence - 0.5));
                
                // 角度差决定补偿方向
                double moveAngleDiff = normalizeAngle(playerMoveAngle - angle);
                if (Math.abs(moveAngleDiff) > Math.PI/2) {
                    predictScale = -predictScale;
                }
                
                targetAngle = normalizeAngle(targetAngle + predictScale);
            }
        }
        
        // 平滑转向 - 减小旋转速度，使运动更平滑
        double rotationSpeed = 0.05 + aggressiveness * 0.1; // 减小旋转速度
        
        if (Math.abs(angleDiff) > minAngleChange) {
            if (angleDiff > 0 && angleDiff <= Math.PI) {
                this.angle += rotationSpeed;
            } else {
                this.angle -= rotationSpeed;
            }
            
            this.angle = normalizeAngle(this.angle);
        } else {
            this.angle = targetAngle;
        }
        
        // 减少精度影响下的随机抖动
        if (precision < 0.9) {
            // 降低抖动幅度，使用更小的随机值
            double jitter = (1.0 - precision) * 0.1 * (random.nextDouble() - 0.5);
            this.angle = normalizeAngle(this.angle + jitter);
        }
    }
    
    /**
     * 检查碰撞
     */
    public boolean checkCollision(int newX, int newY) {
        // 边界检查
        if (newX < 0 || newY < 0 || 
            newX + width > 800 || newY + height > 600) {
            return false;
        }
        
        // 安全检查
        if (detector == null) {
            System.out.println("警告: 碰撞检测器为null，使用默认安全碰撞检测");
            return true;
        }
        
        return !detector.isColliding(newX, newY, width, height);
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
     * 判断是否表现良好
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
     * 更新玩家行为模式
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
     * 标准化模式数据
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
     * 是否应该射击
     */
    private boolean shouldShoot(PlayerTank player, double distance, double angleToPlayer, double levelFactor) {
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime < fireInterval) {
            return false;
        }

        // 基础射击概率（提高到0.6）
        double baseProb = 0.6 * levelFactor;

        // 最佳射击距离
        double optimalDistance = 200;
        double distanceFactor = 1 - Math.min(1, Math.abs(distance - optimalDistance) / 300);

        // 角度差计算
        double angleDiff = Math.abs(normalizeAngle(angle - angleToPlayer));
        double angleFactor = Math.max(0, 1 - (angleDiff / (Math.PI/1.5)));

        // 综合射击概率计算
        double shootProb = baseProb *
                (0.4 + 0.3 * distanceFactor + 0.3 * angleFactor) *
                (0.6 + precision * 0.4) *
                (0.6 + aggressiveness * 0.4) *
                weights.getOrDefault("shoot", 0.9);

        // 特殊情况调整
        if (player.getHealth() < 50) shootProb *= 1.5;
        if (health < 0.3) shootProb *= 0.8;

        // 确保最低射击概率
        return random.nextDouble() < Math.max(0.35, shootProb);
    }
    
    /**
     * 学习系统 - 根据成功/失败调整行为
     */
    public void learn(boolean success, PlayerTank player) {
        // 动态调整学习率
        double dynamicLearningRate = learningRate * (1 + ConfigTool.getLevel() * 0.1);
        double adjustment = success ? dynamicLearningRate : -dynamicLearningRate * 0.5;

        // 更新基础行为权重
        updateBasicWeights(adjustment);
        // 更新基于玩家位置的学习
        updatePositionBasedLearning(player, adjustment);
        // 更新玩家行为模式识别
        updatePatternRecognition(adjustment);
        
        // 场景适应性学习
        adaptToEnvironment(player, success);
        
        // 调整性格特性
        if (random.nextDouble() < 0.2) { // 20%概率调整性格
            adjustPersonality(success);
        }

        normalizeWeights();
        
        // 定期自动保存
        if (random.nextInt(50) == 0) { // 约2%概率保存
            saveLearnedData();
        }
    }
    
    /**
     * 场景适应性学习
     */
    private void adaptToEnvironment(PlayerTank player, boolean success) {
        // 获取玩家和AI的空间关系
        double distance = calculateDistance(player);
        boolean isPlayerNearWall = isNearWall(player.getX(), player.getY(), player.getWidth(), player.getHeight());
        boolean isAINearWall = isNearWall(x, y, width, height);
        
        // 基于空间关系更新权重
        if (isPlayerNearWall && success) {
            // 如果玩家靠近墙壁且AI成功，增强这种情况的权重
            weights.merge("target_near_wall", 0.1, Double::sum);
        }
        
        if (isAINearWall && !success) {
            // 如果AI靠近墙壁且失败，降低这种情况的权重
            weights.merge("avoid_wall_position", 0.1, Double::sum);
        }
        
        // 基于距离学习
        String key;
        if (distance < 150) {
            key = "close_combat";
        } else if (distance < 300) {
            key = "mid_range_combat";
        } else {
            key = "long_range_combat";
        }
        
        double value = success ? 0.05 : -0.03;
        weights.merge(key, value, Double::sum);
    }
    
    /**
     * 检测是否靠近墙壁
     */
    private boolean isNearWall(int x, int y, int width, int height) {
        // 检测点
        int[] probeDistances = {20, 40}; // 检测距离
        int[][] probeDirections = {
            {0, -1}, // 上
            {1, 0},  // 右
            {0, 1},  // 下
            {-1, 0}  // 左
        };
        
        // 中心点
        int centerX = x + width/2;
        int centerY = y + height/2;
        
        // 检查四个方向是否有墙
        for (int[] dir : probeDirections) {
            for (int dist : probeDistances) {
                int probeX = centerX + dir[0] * dist;
                int probeY = centerY + dir[1] * dist;
                
                if (detector != null && detector.isColliding(probeX-5, probeY-5, 10, 10)) {
                    return true; // 靠近墙壁
                }
            }
        }
        
        return false; // 不靠近墙壁
    }
    
    /**
     * 更新基础行为权重
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
     * 更新玩家模式识别
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
     * 调整性格特性
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
            saveData.put("lifetime_shots", lifetimeShots);
            saveData.put("lifetime_hits", lifetimeHits);
            saveData.put("matches_played", matchesPlayed);
            
            oos.writeObject(saveData);
            System.out.println("AI数据已保存到: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存AI数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载学习数据
     */
    @SuppressWarnings("unchecked")
    private void loadLearnedData() {
        File file = new File(DATA_DIR, SAVE_FILE);
        if (!file.exists()) {
            System.out.println("未找到AI数据文件，创建默认权重");
            this.weights = createDefaultWeights();
            return;
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
            } else {
                this.playerPatterns = new HashMap<>();
            }
            
            // 加载权重
            if (saveData.containsKey("weights")) {
                this.weights = (Map<String, Double>) saveData.get("weights");
            } else {
                // 为兼容旧版数据，创建一个新的Map并填充Double类型的值
                this.weights = new HashMap<>();
                for (Map.Entry<String, Object> entry : saveData.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        this.weights.put(entry.getKey(), ((Number)entry.getValue()).doubleValue());
                    }
                }
            }
            
            // 加载生涯统计
            if (saveData.containsKey("lifetime_shots")) {
                this.lifetimeShots = ((Number)saveData.get("lifetime_shots")).intValue();
            }
            
            if (saveData.containsKey("lifetime_hits")) {
                this.lifetimeHits = ((Number)saveData.get("lifetime_hits")).intValue();
            }
            
            if (saveData.containsKey("matches_played")) {
                this.matchesPlayed = ((Number)saveData.get("matches_played")).intValue();
            }
            
            System.out.println("AI数据加载成功");
        } catch (Exception e) {
            System.err.println("加载AI数据失败: " + e.getMessage());
            e.printStackTrace();
            this.weights = createDefaultWeights();
        }
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
     * 对玩家击中注册
     */
    public void registerHit() {
        lifetimeHits++;
        
        // 更新命中率权重
        double hitRatio = lifetimeShots > 0 ? (double)lifetimeHits / lifetimeShots : 0;
        weights.put("shot_accuracy", Math.min(1.0, weights.getOrDefault("shot_accuracy", 0.5) + 0.02));
        weights.put("prediction_accuracy", Math.min(1.0, weights.getOrDefault("prediction_accuracy", 0.5) + 0.01));
        
        // 保存高命中率的学习数据
        if (hitRatio > 0.4 && lifetimeShots > 20) {
            saveLearnedData();
        }
    }
    
    /**
     * 游戏结束时的学习
     */
    public void onMatchEnd(boolean victory, PlayerTank player) {
        // 增加游戏场次计数
        matchesPlayed++;
        
        // 根据胜负学习
        learn(victory, player);
        
        // 强化学习 - 额外奖励/惩罚
        double factor = victory ? 0.3 : -0.15;
        
        // 更新特定权重
        weights.merge("game_sense", factor, Double::sum);
        weights.merge("strategic", factor, Double::sum);
        
        // 根据命中率调整精度特性
        double hitRatio = lifetimeShots > 0 ? (double)lifetimeHits / lifetimeShots : 0;
        if (hitRatio > 0.4) {
            precision = Math.min(1.0, precision + 0.02);
        } else if (hitRatio < 0.2 && lifetimeShots > 30) {
            precision = Math.max(0.3, precision - 0.01);
        }
        
        // 保存学习数据
        saveLearnedData();
    }
    
    /**
     * 死亡时进行学习
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
     * 动态难度调整
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
     * 设置碰撞检测器
     */
    public void setDetector(CollisionDetector detector) {
        this.detector = detector;
    }
    
    /**
     * 动态难度适应系统 - 根据当前关卡调整AI能力
     */
    public void adaptToDifficulty(int currentLevel) {
        // 基础难度参数
        double baseDifficulty = 0.5;
        // 难度增长系数
        double difficultyGrowth = 0.08;
        // 计算当前难度
        double currentDifficulty = Math.min(0.95, baseDifficulty + (currentLevel - 1) * difficultyGrowth);
        
        // 根据难度调整AI特性
        aggressiveness = Math.min(1.0, weights.getOrDefault("personality_aggressive", 0.7) + currentDifficulty * 0.3);
        intelligence = Math.min(1.0, weights.getOrDefault("personality_intelligence", 0.6) + currentDifficulty * 0.4);
        precision = Math.min(1.0, weights.getOrDefault("personality_precision", 0.8) + currentDifficulty * 0.2);
        
        // 调整行为权重
        weights.put("shoot", Math.min(1.0, weights.getOrDefault("shoot", 0.85) + currentDifficulty * 0.15));
        weights.put("predict", Math.min(1.0, weights.getOrDefault("predict", 0.4) + currentDifficulty * 0.3));
        
        // 调整反应时间和行动频率
        actionDelay = Math.max(50, 100 - currentLevel * 5); // 越高级反应越快
        // Cannot modify FIRE_INTERVAL as it's final, you need to handle this differently
        
        // 调整移动速度
        currentSpeed = BASE_SPEED + Math.min(3, currentLevel / 2);
        
        // 日志记录
        System.out.println("AI适应到难度等级" + currentLevel + 
                          "，攻击性:" + String.format("%.2f", aggressiveness) + 
                          "，智能:" + String.format("%.2f", intelligence) + 
                          "，精度:" + String.format("%.2f", precision));
    }
    
    // 标准化角度到0-2π范围
    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }
    
    // 实现接口方法
    @Override
    public void fire() {
        // 空实现，使用带玩家参数的版本
    }

    @Override
    public void fire(PlayerTank player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime >= fireInterval) {
            // 计算炮管前端位置 - 保证子弹从正前方射出
            int barrelLength = width / 2 + 5;
            int bulletX = (int) (x + width / 2 + Math.cos(angle) * barrelLength);
            int bulletY = (int) (y + height / 2 + Math.sin(angle) * barrelLength);

            // 检查子弹生成位置是否与玩家坦克重叠
            Rectangle bulletBounds = new Rectangle(bulletX-5, bulletY-5, 10, 10);
            if (player != null && player.isAlive() && 
                player.getCollisionBounds().intersects(bulletBounds)) {
                return; // 取消本次射击
            }

            // 创建子弹 - 使用坦克当前角度作为射击方向
            double spreadFactor = (1.0 - precision) * 0.2;
            double randomSpread = (random.nextDouble() - 0.5) * spreadFactor;
            double fireAngle = angle + randomSpread + predictFactor;
            
            EnemyBullet bullet = new EnemyBullet(bulletX, bulletY, fireAngle);
            bullet.setMinCollisionDistance(30); // 设置最小碰撞检测距离
            bullets.add(bullet);
            lastFireTime = currentTime;
            lifetimeShots++;
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
        if (normAngle >= 7*Math.PI/4 || normAngle < Math.PI/4) return 0; // 上
        if (normAngle >= Math.PI/4 && normAngle < 3*Math.PI/4) return 1; // 右
        if (normAngle >= 3*Math.PI/4 && normAngle < 5*Math.PI/4) return 2; // 下
        return 3; // 左
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

    @Override
    public Image getCurrentImage() {
        return tankImage;
    }
    
    @Override
    public double getAngle() {
        return this.angle;
    }
    
    /**
     * 绘制坦克
     */
    public void draw(Graphics g) {
        if (!isAlive() || tankImage == null) return;
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 坦克中心点
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        
        // 变换坐标系到坦克中心
        g2d.translate(centerX, centerY);
        
        // 旋转 - 图片默认朝右，角度定义为0度朝右，直接使用当前角度
        g2d.rotate(angle);
        
        // 绘制坦克
        g2d.drawImage(tankImage, -width / 2, -height / 2, width, height, null);
        
        g2d.dispose();
        
        // 绘制子弹
        drawBullets(g);
    }

    @Override
    protected void drawTank(Graphics g) {

    }

    /**
     * 绘制调试信息
     */
    private void drawDebugInfo(Graphics2D g2d) {
        // 绘制不同行为状态的视觉指示
        switch (currentBehaviorState) {
            case ATTACKING:
                g2d.setColor(new Color(255, 0, 0, 90));
                g2d.fillOval(-20, -20, 40, 40);
                break;
            case EVADING:
                g2d.setColor(new Color(0, 0, 255, 90));
                g2d.fillOval(-20, -20, 40, 40);
                break;
            case STRATEGIC:
                g2d.setColor(new Color(0, 255, 0, 90));
                g2d.fillOval(-20, -20, 40, 40);
                break;
        }
        
        // 绘制健康状态条
        int healthBarWidth = 40;
        int healthBarHeight = 5;
        g2d.setColor(Color.BLACK);
        g2d.drawRect(-healthBarWidth/2, -height/2 - 10, healthBarWidth, healthBarHeight);
        g2d.setColor(new Color(255, (int)(health * 255), 0));
        g2d.fillRect(-healthBarWidth/2, -height/2 - 10, (int)(healthBarWidth * health), healthBarHeight);
    }
    
    /**
     * 绘制子弹
     */
    public void drawBullets(Graphics g) {
        for (EnemyBullet bullet : bullets) {
            bullet.draw(g);
        }
    }
    
    /**
     * 获取AI状态调试信息
     */
    public String getAIDebugInfo() {
        if (!ConfigTool.isDebugMode()) return "";
        
        StringBuilder info = new StringBuilder();
        info.append("AI状态: ").append(getBehaviorName()).append("\n");
        info.append("生命值: ").append(health).append("\n");
        info.append("攻击性: ").append(String.format("%.2f", aggressiveness)).append("\n");
        info.append("智能: ").append(String.format("%.2f", intelligence)).append("\n");
        info.append("精度: ").append(String.format("%.2f", precision)).append("\n");
        info.append("学习场次: ").append(matchesPlayed).append("\n");
        info.append("命中率: ").append(lifetimeShots > 0 ? 
                    String.format("%.2f", (double)lifetimeHits/lifetimeShots) : "N/A");
        
        return info.toString();
    }
    
    /**
     * 获取行为状态名称
     */
    private String getBehaviorName() {
        switch(currentBehaviorState) {
            case ATTACKING: return "攻击";
            case EVADING: return "躲避";
            case STRATEGIC: return "策略";
            default: return "正常";
        }
    }
    
    /**
     * 获取子弹列表
     */
    public List<EnemyBullet> getBullets() {
        return bullets;
    }
    
    // 在EnemyTank和AITank的takeDamage方法中确保调用startExplosion
    @Override
    public void takeDamage(int damage) {
        if (alive && health > 0) {
            health -= damage;
            
            if (health <= 0) {
                // 保存当前位置用于爆炸效果
                final int explosionX = x + width/2;
                final int explosionY = y + height/2;
                final int explosionSize = Math.max(width, height) * 2;
                
                // 记录子弹并转移到孤儿子弹列表
                List<EnemyBullet> activeBullets = new ArrayList<>(bullets);
                bullets.clear();
                
                // 将子弹添加到PVPMode或PVEMode的孤儿子弹列表中
                if (bullets != null && !bullets.isEmpty()) {
                    // 在Mode类中需要提供一个添加孤儿子弹的方法
                    // 例如: pvpMode.addOrphanedBullets(activeBullets);
                }
                
                // 标记为死亡
                alive = false;
                
                // 创建爆炸效果
                ExplosionManager.getInstance().createExplosion(explosionX, explosionY, explosionSize);
                
                System.out.println("坦克被击毁，触发爆炸效果 at " + explosionX + "," + explosionY);
            }
        }
    }

    /**
     * 高级学习系统 - 引入记忆和遗忘机制
     */
    private Map<String, Double> shortTermMemory = new HashMap<>();
    private Map<String, Double> longTermMemory = new HashMap<>();

    /**
     * 高级学习方法 - 整合短期记忆和长期记忆
     */
    public void advancedLearn(boolean success, PlayerTank player) {
        // 常规学习
        learn(success, player);
        
        // 短期记忆学习 - 基于最近几次交互
        String memoryKey = generateSituationKey(player);
        double result = success ? 1.0 : -0.5;
        
        // 更新短期记忆
        shortTermMemory.merge(memoryKey, result, (old, val) -> old * 0.7 + val * 0.3);
        
        // 将重要的短期记忆转移到长期记忆
        shortTermMemory.forEach((key, value) -> {
            if (Math.abs(value) > 0.7) { // 强烈的记忆会转入长期记忆
                longTermMemory.merge(key, value * 0.3, (old, val) -> old * 0.9 + val * 0.1);
            }
        });
        
        // 根据长期记忆调整策略权重
        updateWeightsFromMemory();
        
        // 定期清理短期记忆
        if (random.nextInt(20) == 0) {
            shortTermMemory.entrySet().removeIf(entry -> Math.abs(entry.getValue()) < 0.3);
        }
    }

    /**
     * 生成情境键 - 描述当前战斗情况
     */
    private String generateSituationKey(PlayerTank player) {
        double distance = calculateDistance(player);
        String distanceKey = distance < 150 ? "close" : (distance < 300 ? "medium" : "far");
        
        boolean playerNearWall = isNearWall(player.getX(), player.getY(), player.getWidth(), player.getHeight());
        boolean aiNearWall = isNearWall(x, y, width, height);
        
        int healthDiff = (int)(health * 100) - player.getHealth();
        String healthKey = healthDiff > 20 ? "advantage" : (healthDiff < -20 ? "disadvantage" : "equal");
        
        return distanceKey + "_" + (playerNearWall ? "pWall" : "pOpen") + 
               "_" + (aiNearWall ? "aWall" : "aOpen") + "_" + healthKey;
    }

    /**
     * 根据记忆更新权重
     */
    private void updateWeightsFromMemory() {
        // 将长期记忆中的成功经验转化为行为权重
        longTermMemory.forEach((key, value) -> {
            if (value > 0) {
                if (key.startsWith("close")) {
                    weights.merge("close_combat", value * 0.1, Double::sum);
                } else if (key.startsWith("medium")) {
                    weights.merge("mid_range_combat", value * 0.1, Double::sum);
                } else if (key.startsWith("far")) {
                    weights.merge("long_range_combat", value * 0.1, Double::sum);
                }
                
                if (key.contains("pWall")) {
                    weights.merge("target_near_wall", value * 0.1, Double::sum);
                }
                
                if (key.contains("aWall") && value < 0) {
                    weights.merge("avoid_wall_position", 0.1, Double::sum);
                }
            }
        });
    }

    /**
     * 扩展行为状态 - 添加高级行为
     */
    private enum ExtendedBehaviorState {
        NORMAL, ATTACKING, EVADING, STRATEGIC,
        AMBUSH,        // 设置伏击
        FLANKING,      // 侧翼攻击
        BAITING,       // 诱敌
        DEFENSIVE      // 防御姿态
    }

    /**
     * 高级策略行为 - 侧翼攻击
     */
    private void flankingManeuver(PlayerTank player, double levelFactor) {
        // 计算玩家朝向
        double playerAngle = player.getAngle();
        
        // 计算侧翼位置 (90度角)
        double flankAngle = playerAngle + Math.PI/2;
        if (random.nextBoolean()) {
            flankAngle = playerAngle - Math.PI/2;
        }
        
        // 计算侧翼目标位置
        double flankDistance = 200 + random.nextInt(100);
        double targetX = player.getX() + Math.cos(flankAngle) * flankDistance;
        double targetY = player.getY() + Math.sin(flankAngle) * flankDistance;
        
        // 移动到侧翼位置
        moveToPosition((int)targetX, (int)targetY, levelFactor * 1.2);
        
        // 保持面向玩家
        double angleToPlayer = calculateAngleToPlayer(player);
        this.angle = angleToPlayer;
    }

    // 伏击行为相关变量
    private boolean hasGoodAmbushPosition = false;
    private int ambushX = 0;
    private int ambushY = 0;
    
    /**
     * 高级策略行为 - 伏击模式
     */
    private void ambushBehavior(PlayerTank player, double levelFactor) {
        // 寻找合适的伏击位置 (靠近墙体)
        if (!hasGoodAmbushPosition) {
            findAmbushPosition();
            return;
        }
        
        // 如果玩家距离伏击点足够近，开始攻击
        double distanceToPlayer = calculateDistance(player);
        if (distanceToPlayer < 250) {
            // 切换为攻击模式
            currentBehaviorState = BehaviorState.ATTACKING;
            attackPlayer(player, distanceToPlayer, levelFactor * 1.5);
        } else {
            // 继续保持伏击位置
            moveToPosition(ambushX, ambushY, levelFactor * 0.5);
            // 面向玩家方向
            this.angle = calculateAngleToPlayer(player);
        }
    }
    
    /**
     * 寻找适合伏击的位置
     */
    private boolean findAmbushPosition() {
        // 在地图周围寻找靠近墙壁的位置
        int[] possibleX = {100, 200, 600, 700, x};
        int[] possibleY = {100, 200, 400, 500, y};
        
        // 尝试不同的位置组合
        for (int i = 0; i < possibleX.length; i++) {
            for (int j = 0; j < possibleY.length; j++) {
                int testX = possibleX[i];
                int testY = possibleY[j];
                
                // 检查位置是否有效
                if (testX >= 0 && testX < 800 - width && 
                    testY >= 0 && testY < 600 - height) {
                    
                    // 检查是否靠近墙
                    if (isNearWall(testX, testY, width, height)) {
                        ambushX = testX;
                        ambushY = testY;
                        hasGoodAmbushPosition = true;
                        return true;
                    }
                }
            }
        }
        
        // 如果找不到好位置，使用当前位置
        ambushX = x;
        ambushY = y;
        hasGoodAmbushPosition = isNearWall(x, y, width, height);
        return hasGoodAmbushPosition;
    }
    
    // 诱敌战术相关变量
    private int baitingPhase = -1;
    private long baitingStartTime = 0;
    
    // 诱敌战术方法已在上面定义，这里删除重复定义

    /**
     * 高级策略行为 - 诱敌战术
     */
    private void baitingTactic(PlayerTank player, double levelFactor) {
        // 初始阶段 - 假装逃跑
        if (baitingPhase == 0) {
            evadeFrom(player, levelFactor * 0.8);
            
            // 当与玩家距离足够远时，切换到第二阶段
            if (calculateDistance(player) > 300) {
                baitingPhase = 1;
            }
        } 
        // 第二阶段 - 停止并准备反击
        else if (baitingPhase == 1) {
            // 转向玩家
            this.angle = calculateAngleToPlayer(player);
            
            // 如果玩家追过来，切换到反击阶段
            if (calculateDistance(player) < 250) {
                baitingPhase = 2;
            }
        }
        // 反击阶段
        else {
            attackPlayer(player, calculateDistance(player), levelFactor * 1.4);
            
            // 反击一段时间后重置战术
            if (System.currentTimeMillis() - baitingStartTime > 5000) {
                baitingPhase = 0;
                baitingStartTime = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * 高级移动系统 - 移动到指定位置
     */
    private void moveToPosition(int targetX, int targetY, double speedFactor) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 5) { // 只有距离足够远才移动
            // 计算移动角度
            double moveAngle = Math.atan2(dy, dx);
            
            // 使用平滑转向
            smoothRotateToAngle(moveAngle, 0.1);
            
            // 计算新位置
            int newX = (int)(x + Math.cos(this.angle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(this.angle) * currentSpeed * speedFactor);

            // 碰撞检测
            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            } else {
                // 如果碰到障碍，尝试智能绕行
                smartPathfinding(targetX, targetY, speedFactor);
            }
        }
    }

    /**
     * 智能路径寻找 - 简化版本
     */
    private void smartPathfinding(int targetX, int targetY, double speedFactor) {
        // 尝试8个方向中的一个来绕过障碍
        double[] testAngles = {
            angle + Math.PI/4, angle - Math.PI/4,
            angle + Math.PI/2, angle - Math.PI/2,
            angle + 3*Math.PI/4, angle - 3*Math.PI/4,
            angle + Math.PI, angle
        };
        
        for (double testAngle : testAngles) {
            int newX = (int)(x + Math.cos(testAngle) * currentSpeed * speedFactor);
            int newY = (int)(y + Math.sin(testAngle) * currentSpeed * speedFactor);
            
            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
                this.angle = testAngle;
                return;
            }
        }
    }

    /**
     * 平滑转向目标角度
     */
    private void smoothRotateToAngle(double targetAngle, double rotationSpeed) {
        double angleDiff = normalizeAngle(targetAngle - this.angle);
        
        if (Math.abs(angleDiff) < 0.05) {
            this.angle = targetAngle;
            return;
        }
        
        if (angleDiff > 0 && angleDiff <= Math.PI) {
            this.angle += rotationSpeed;
        } else {
            this.angle -= rotationSpeed;
        }
        
        this.angle = normalizeAngle(this.angle);
    }
    
    /**
     * 高级预测射击系统
     */
    private void advancedPredictiveShot(PlayerTank player) {
        if (!isAlive() || !player.isAlive()) return;
        
        // 基础预测 - 考虑当前玩家速度和方向
        double playerSpeed = Math.sqrt(
            Math.pow(player.getX() - lastPlayerX, 2) + 
            Math.pow(player.getY() - lastPlayerY, 2)
        );
        
        // 子弹飞行时间估计
        double distance = calculateDistance(player);
        double bulletSpeed = 10; // 子弹速度
        double timeToTarget = distance / bulletSpeed;
        
        // 计算玩家移动方向
        double playerMoveAngle = 0;
        if (playerSpeed > 0.5) {
            playerMoveAngle = Math.atan2(
                player.getY() - lastPlayerY,
                player.getX() - lastPlayerX
            );
        } else {
            // 如果玩家基本静止，使用玩家朝向作为预测依据
            playerMoveAngle = player.getAngle();
            playerSpeed = 1; // 假设会开始移动
        }
        
        // 预测玩家未来位置
        double predictedX = player.getX() + Math.cos(playerMoveAngle) * playerSpeed * timeToTarget * intelligence;
        double predictedY = player.getY() + Math.sin(playerMoveAngle) * playerSpeed * timeToTarget * intelligence;
        
        // 计算预测瞄准角度
        double predictedAngle = Math.atan2(predictedY - y, predictedX - x);
        
        // 应用精度因子
        double accuracyFactor = 1.0 - (1.0 - precision) * 0.3;
        double finalAngle = predictedAngle * accuracyFactor + angle * (1 - accuracyFactor);
        
        // 应用角度预测
        this.predictFactor = normalizeAngle(finalAngle - angle);
        
        // 发射子弹
        fire(player);
        
        // 重置预测因子
        this.predictFactor = 0;
    }

    /**
     * 高级决策系统 - 基于当前情境和历史数据
     */
    private void advancedBehaviorDecision(PlayerTank player, double distance, double threatLevel, int currentLevel) {
        // 冷却检查
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStateChangeTime < STATE_CHANGE_COOLDOWN) {
            return;
        }
        
        // 高级状态选择 - 考虑更多因素
        double[] stateWeights = new double[BehaviorState.values().length];
        
        // 根据距离计算各状态权重
        if (distance < 150) {
            stateWeights[BehaviorState.ATTACKING.ordinal()] += 0.4 * aggressiveness;
            stateWeights[BehaviorState.EVADING.ordinal()] += 0.3 * (1 - aggressiveness);
            stateWeights[BehaviorState.STRATEGIC.ordinal()] += 0.2 * intelligence;
        } else if (distance < 300) {
            stateWeights[BehaviorState.ATTACKING.ordinal()] += 0.3;
            stateWeights[BehaviorState.STRATEGIC.ordinal()] += 0.4 * intelligence;
            stateWeights[BehaviorState.EVADING.ordinal()] += 0.2;
        } else {
            stateWeights[BehaviorState.ATTACKING.ordinal()] += 0.5;
            stateWeights[BehaviorState.STRATEGIC.ordinal()] += 0.3 * intelligence;
        }
        
        // 考虑威胁级别
        if (threatLevel > 0.7) {
            stateWeights[BehaviorState.EVADING.ordinal()] += 0.4 * (1 - aggressiveness);
        }
        
        // 考虑生命值
        if (health < 0.4) {
            stateWeights[BehaviorState.EVADING.ordinal()] += 0.3;
            stateWeights[BehaviorState.ATTACKING.ordinal()] -= 0.2;
        }
        
        // 考虑关卡难度
        if (currentLevel > 3) {
            // 高级别时更具侵略性
            stateWeights[BehaviorState.ATTACKING.ordinal()] += 0.1 * (currentLevel - 3);
            // 更多使用策略行为
            stateWeights[BehaviorState.STRATEGIC.ordinal()] += 0.05 * (currentLevel - 3);
        }
        
        // 应用学习权重
        stateWeights[BehaviorState.ATTACKING.ordinal()] *= weights.getOrDefault("chase", 0.9);
        stateWeights[BehaviorState.EVADING.ordinal()] *= weights.getOrDefault("evade", 0.4);
        stateWeights[BehaviorState.STRATEGIC.ordinal()] *= weights.getOrDefault("strategic", 0.5);
        
        // 找出权重最高的状态
        int maxIndex = 0;
        for (int i = 1; i < stateWeights.length; i++) {
            if (stateWeights[i] > stateWeights[maxIndex]) {
                maxIndex = i;
            }
        }
        
        // 设置新状态
        BehaviorState newState = BehaviorState.values()[maxIndex];
        
        // 如果状态改变，记录时间
        if (currentBehaviorState != newState) {
            previousState = currentBehaviorState;
            currentBehaviorState = newState;
            lastStateChangeTime = currentTime;
            
            // 记录状态变化到学习系统
            String stateKey = "state_change_" + previousState + "_to_" + currentBehaviorState;
            weights.merge(stateKey, 0.1, Double::sum);
        }
    }
    
    /**
     * 随机高级行为 - 增加不可预测性
     */
    private void executeRandomAdvancedBehavior(PlayerTank player, double levelFactor, int currentLevel) {
        // 只在较高级别时使用高级行为
        if (currentLevel < 3) return;
        
        // 随机选择高级行为
        int behaviorChoice = random.nextInt(100);
        
        // 行为选择概率随关卡提高而增加
        int advancedBehaviorChance = 10 + (currentLevel - 3) * 5;
        
        if (behaviorChoice < advancedBehaviorChance) {
            switch (random.nextInt(3)) {
                case 0:
                    flankingManeuver(player, levelFactor);
                    break;
                case 1:
                    if (baitingPhase == -1) {
                       
                        baitingPhase = 0;
                        baitingStartTime = System.currentTimeMillis();
                    }
                    baitingTactic(player, levelFactor);
                    break;
                case 2:
                    if (hasGoodAmbushPosition || findAmbushPosition()) {
                        ambushBehavior(player, levelFactor);
                    } else {
                        strategicMovement(player, levelFactor);
                    }
                    break;
            }
        } else {
            // 执行常规行为
            executeBehavior(player, calculateDistance(player), levelFactor);
        }
    }
}