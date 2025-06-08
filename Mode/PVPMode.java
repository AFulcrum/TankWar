package Mode;

import Config.*;
import InterFace.CollisionDetector;
import InterFace.Bullet;
import Structure.PVPWall;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class PVPMode extends JPanel {
    private PlayerTank player;
    private java.util.List<EnemyTank> enemies = new ArrayList<>();  // 敌方坦克列表
    private Timer gameTimer;
    private boolean gameRunning = false;
    private CollisionDetector detector;
    private java.util.List<PVPWall> PVPWalls = new ArrayList<>(); // 添加墙体列表
    private JLabel beatNumLabel;
    private JLabel healthLabel;
    private List<EnemyBullet> orphanedBullets = new ArrayList<>(); // 孤立子弹列表

    // 添加以下成员变量到类顶部
    private boolean isCountingDown = false;
    private int countDownSeconds = 3;
    private Timer countDownTimer;
    private long countDownStartTime;


    public PVPMode(CollisionDetector collisionDetector, JLabel beatLabel, JLabel healthLabel) {
        this.detector = collisionDetector;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocusInWindow();
        this.beatNumLabel = beatLabel;
        this.healthLabel = healthLabel;

        // 初始化墙体
        initWalls();

        // 创建玩家坦克（暂时放在默认位置，后续再调整）
        player = new PlayerTank(50, 50, collisionDetector);

        // 更新碰撞检测器
        if (detector instanceof SimpleCollisionDetector) {
            ((SimpleCollisionDetector) detector).setWalls(PVPWalls);
        }

        // 设置键盘监听
        setupKeyBindings();

        // 游戏循环
        gameTimer = new Timer(16, e -> {
            if (gameRunning) {
                updateGame();
                repaint();
            }
        });

        // 添加组件监听器，当窗口大小确定后再创建敌方坦克
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateCollisionBoundary();
                // 重新初始化墙体
                PVPWalls.clear();
                initWalls();
                if (detector instanceof SimpleCollisionDetector) {
                    ((SimpleCollisionDetector) detector).setWalls(PVPWalls);
                }

                // 只有第一次调整大小时初始化坦克位置
                if (enemies.isEmpty() && getWidth() > 100 && getHeight() > 100) {
                    // 调整玩家坦克位置
                    repositionPlayerTank();
                    // 创建敌方坦克
                    createInitialEnemies();
                }
            }
        });

        repaint(); // 保证初始显示
    }

    private void updateGame() {
        if (!gameRunning) return;

        // 添加玩家存活状态检查
        if (player != null && player.getHealth() <= 0) {
            gameOver();
            return;
        }

        player.updateMovement(); // 更新玩家坦克
        player.updateBullets(); // 更新玩家子弹

        // 更新所有敌方坦克
        for (EnemyTank enemy : enemies) {
            enemy.update();      // 更新敌方坦克(包括移动和射击)
            enemy.updateBullets(); // 更新敌方子弹
        }
        
        // 更新孤儿子弹
        for (int i = orphanedBullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = orphanedBullets.get(i);
            bullet.updatePosition();
            
            // 检查是否已失活或超出边界
            if (!bullet.isActive() || isOutOfBounds(bullet)) {
                orphanedBullets.remove(i);
            }
        }
        
        // 检查子弹与墙体/边界的碰撞
        checkBulletWallCollisions();
        // 检查子弹与坦克的碰撞
        checkBulletCollisions();

        // 更新爆炸效果
        ExplosionManager.getInstance().update();
        
        repaint();
    }

    // 添加边界检查方法
    private boolean isOutOfBounds(EnemyBullet bullet) {
        Rectangle bounds = bullet.getCollisionBounds();
        if (bounds == null) return true;
        return bounds.x < 0 || bounds.y < 0 ||
               bounds.x > getWidth() || bounds.y > getHeight();
    }

    // 重新定位玩家坦克到随机位置
    private void repositionPlayerTank() {
        if (getWidth() <= 100 || getHeight() <= 100) return;

        // 计算游戏区域的中心点
        int centerX = getWidth() / 2 - player.getWidth() / 2;
        int centerY = getHeight() / 2 - player.getHeight() / 2;

        // 将玩家坦克放置在中心位置
        player.setPosition(centerX, centerY);
    }

    // 创建初始敌方坦克
    private void createInitialEnemies() {
        if (getWidth() <= 100 || getHeight() <= 100) return;

        Random rand = new Random();
        int enemyCount = rand.nextInt(3) + 1; // 1到3之间的随机数

        for (int i = 0; i < enemyCount; i++) {
            createEnemy();
        }
    }
    // 添加创建敌人的方法
    private void createEnemy() {
        if (getWidth() <= 100 || getHeight() <= 100 || player == null) return;

        Random rand = new Random();
        int maxAttempts = 10; // 设置最大尝试次数，防止无限循环
        int attempts = 0;

        int enemyX = rand.nextInt(Math.max(1, getWidth() - 80));
        int enemyY = rand.nextInt(Math.max(1, getHeight() - 80));

        // 确保敌人不会与玩家重叠，但限制尝试次数
        while (Math.abs(enemyX - player.getX()) < 100 &&
                Math.abs(enemyY - player.getY()) < 100 &&
                attempts < maxAttempts) {
            enemyX = rand.nextInt(Math.max(1, getWidth() - 80));
            enemyY = rand.nextInt(Math.max(1, getHeight() - 80));
            attempts++;
        }

        enemies.add(new EnemyTank(enemyX, enemyY, detector));
    }

    // 初始化墙体
    private void initWalls() {
        // 只有当游戏区域大小足够时才初始化墙体
        if (getWidth() > 0 && getHeight() > 0) {
            // 添加边界墙体
            PVPWalls.addAll(PVPWall.createBoundaryWalls(getWidth(), getHeight()));

            // 使用结构化墙体替代随机墙体
            PVPWalls.addAll(PVPWall.createStructuredWalls(getWidth(), getHeight()));
        }
    }

    private void updateCollisionBoundary() {
        if (detector instanceof SimpleCollisionDetector) {
            ((SimpleCollisionDetector)detector).setGameAreaSize(getSize());
        }
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // 玩家移动控制
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "p_up_press");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "p_up_release");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "p_down_press");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "p_down_release");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "p_left_press");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "p_left_release");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "p_right_press");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "p_right_release");
        // 玩家射击和技能
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "p_fire");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0, false), "p_skill");
        // 添加动作
        am.put("p_up_press", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyPress(KeyEvent.VK_UP); }
        });
        am.put("p_up_release", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyRelease(KeyEvent.VK_UP); }
        });
        am.put("p_down_press", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyPress(KeyEvent.VK_DOWN); }
        });
        am.put("p_down_release", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyRelease(KeyEvent.VK_DOWN); }
        });
        am.put("p_left_press", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyPress(KeyEvent.VK_LEFT); }
        });
        am.put("p_left_release", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyRelease(KeyEvent.VK_LEFT); }
        });
        am.put("p_right_press", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyPress(KeyEvent.VK_RIGHT); }
        });
        am.put("p_right_release", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyRelease(KeyEvent.VK_RIGHT); }
        });
        am.put("p_fire", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyPress(KeyEvent.VK_SPACE); }
        });
        am.put("p_skill", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { player.handleKeyPress(KeyEvent.VK_X); }
        });

    }

    private void updateDisplays() {
        if (beatNumLabel != null) {
            beatNumLabel.setText("<html><div style='text-align: center;'>击<br>败<br>数<br>"
                    + ConfigTool.getBeatNum() + "</html>");
        }
        if (healthLabel != null) {
            healthLabel.setText("<html><div style='text-align: center;'>生<br>命<br>值<br>"
                    + PlayerTank.getHealth() + "<br>---</html>");
        }
    }
    /**
     * 优化的子弹与墙体碰撞检测系统
     */
    private void checkBulletWallCollisions() {
        // 检测玩家子弹与墙体碰撞
        for (PlayerBullet bullet : player.getBullets()) {
            if (!bullet.isActive()) continue;

            Rectangle bulletBounds = bullet.getCollisionBounds();
            if (bulletBounds == null) continue;
            
            // 获取子弹中心点
            int bulletCenterX = bulletBounds.x + bulletBounds.width / 2;
            int bulletCenterY = bulletBounds.y + bulletBounds.height / 2;
            
            // 即时边界检查 - 防止子弹已经越过边界
            if (checkAndHandleBoundaryCollision(bullet, bulletCenterX, bulletCenterY)) {
                continue; // 如果发生边界碰撞，跳过后续检测
            }
            
            // 获取子弹当前方向和速度
            double angle = bullet.getAngle();
            double speed = bullet.getSpeed();
            double dx = Math.sin(angle);
            double dy = -Math.cos(angle);
            
            // 预测下一位置 - 增加预测系数，解决高速穿墙问题
            double predictionFactor = Math.min(1.5, 1 + speed / 20.0);
            int nextX = (int)(bulletCenterX + dx * speed * predictionFactor);
            int nextY = (int)(bulletCenterY + dy * speed * predictionFactor);
            
            // 使用改进的射线检测与墙体碰撞
            BulletCollisionResult collision = detectBulletCollision(
                bulletCenterX, bulletCenterY, nextX, nextY, bullet.getRadius());
            
            // 处理碰撞结果
            if (collision.hasCollided) {
                if (bullet.canBounce()) {
                    processBulletBounce(bullet, collision);
                } else {
                    bullet.deactivate();
                }
            }
        }
        
        // 检测敌方坦克子弹与墙体碰撞
        for (EnemyTank enemy : enemies) {
            for (EnemyBullet bullet : enemy.getBullets()) {
                if (!bullet.isActive()) continue;

                Rectangle bulletBounds = bullet.getCollisionBounds();
                if (bulletBounds == null) continue;
                
                // 获取子弹中心点
                int bulletCenterX = bulletBounds.x + bulletBounds.width / 2;
                int bulletCenterY = bulletBounds.y + bulletBounds.height / 2;
                
                // 即时边界检查 - 防止子弹已经越过边界
                if (checkAndHandleBoundaryCollision(bullet, bulletCenterX, bulletCenterY)) {
                    continue; // 如果发生边界碰撞，跳过后续检测
                }
                
                // 获取子弹当前方向和速度
                double angle = bullet.getAngle();
                double speed = bullet.getSpeed();
                double dx = Math.cos(angle);  // 注意敌方子弹使用cos角度
                double dy = Math.sin(angle);  // 使用sin角度
                
                // 预测下一位置
                double predictionFactor = Math.min(1.5, 1 + speed / 20.0);
                int nextX = (int)(bulletCenterX + dx * speed * predictionFactor);
                int nextY = (int)(bulletCenterY + dy * speed * predictionFactor);
                
                // 使用射线检测与墙体碰撞
                BulletCollisionResult collision = detectBulletCollision(
                    bulletCenterX, bulletCenterY, nextX, nextY, bullet.getRadius());
                
                // 处理碰撞结果
                if (collision.hasCollided) {
                    if (bullet.canBounce()) {
                        processEnemyBulletBounce(bullet, collision);
                    } else {
                        bullet.deactivate();
                    }
                }
            }
        }
        
        // 处理孤儿子弹与墙体的碰撞
        for (EnemyBullet bullet : orphanedBullets) {
            if (!bullet.isActive()) continue;

            Rectangle bulletBounds = bullet.getCollisionBounds();
            if (bulletBounds == null) continue;
            
            // 获取子弹中心点
            int bulletCenterX = bulletBounds.x + bulletBounds.width / 2;
            int bulletCenterY = bulletBounds.y + bulletBounds.height / 2;
            
            // 即时边界检查
            if (checkAndHandleBoundaryCollision(bullet, bulletCenterX, bulletCenterY)) {
                continue;
            }
            
            // 获取子弹当前方向和速度
            double angle = bullet.getAngle();
            double speed = bullet.getSpeed();
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            
            // 预测下一位置
            double predictionFactor = Math.min(1.5, 1 + speed / 20.0);
            int nextX = (int)(bulletCenterX + dx * speed * predictionFactor);
            int nextY = (int)(bulletCenterY + dy * speed * predictionFactor);
            
            // 使用射线检测与墙体碰撞
            BulletCollisionResult collision = detectBulletCollision(
                bulletCenterX, bulletCenterY, nextX, nextY, bullet.getRadius());
            
            // 处理碰撞结果
            if (collision.hasCollided) {
                if (bullet.canBounce()) {
                    processEnemyBulletBounce(bullet, collision);
                } else {
                    bullet.deactivate();
                }
            }
        }
    }

    /**
     * 检测子弹与墙体和边界的碰撞
     */
    private BulletCollisionResult detectBulletCollision(
            double startX, double startY, double endX, double endY, double bulletRadius) {
        
        BulletCollisionResult result = new BulletCollisionResult();
        
        // 子弹移动向量
        double moveX = endX - startX;
        double moveY = endY - startY;
        double moveLength = Math.sqrt(moveX * moveX + moveY * moveY);
        
        // 动态调整采样点数量，基于移动距离
        int samplePoints = 3; // 基础采样点
        if (moveLength > 15) samplePoints = 5;
        if (moveLength > 25) samplePoints = 7;
        
        // 标准化移动向量
        if (moveLength > 0) {
            moveX /= moveLength;
            moveY /= moveLength;
        }
        
        // 使用多个采样点检查碰撞，提高精度
        for (int i = 0; i <= samplePoints; i++) {
            double t = (double) i / samplePoints;
            double sampleX = startX + t * (endX - startX);
            double sampleY = startY + t * (endY - startY);
            
            // 检查此采样点与所有墙体的碰撞
            for (PVPWall wall : PVPWalls) {
                Rectangle wallBounds = wall.getCollisionBounds();
                checkSegmentCollision(result, sampleX, sampleY, endX, endY, bulletRadius, wallBounds, moveX, moveY);
                
                // 如果已检测到碰撞，停止检查
                if (result.hasCollided) break;
            }
            
            // 如果在此采样点发现碰撞，停止检查后续点
            if (result.hasCollided) break;
        }
        
        // 检查游戏边界 - 如果没有墙体碰撞
        if (!result.hasCollided) {
            checkBoundaryCollision(result, startX, startY, endX, endY, bulletRadius);
        }
        
        return result;
    }

    /**
     * 专门检查和处理子弹与边界的碰撞
     * @return 是否发生碰撞
     */
    private boolean checkAndHandleBoundaryCollision(Bullet bullet, int centerX, int centerY) {
        int gameWidth = getWidth();
        int gameHeight = getHeight();
        int radius = bullet instanceof PlayerBullet ? 
                     ((PlayerBullet)bullet).getRadius() : 
                     ((EnemyBullet)bullet).getRadius();
        
        // 精确检测边界碰撞
        boolean collided = false;
        BulletCollisionResult result = new BulletCollisionResult();
        result.hasCollided = true;
        result.isBoundary = true;
        
        // 左边界碰撞
        if (centerX - radius <= 0) {
            result.collisionX = radius;
            result.collisionY = centerY;
            result.normalX = 1;
            result.normalY = 0;
            result.penetrationDepth = radius;
            collided = true;
        }
        // 右边界碰撞
        else if (centerX + radius >= gameWidth) {
            result.collisionX = gameWidth - radius;
            result.collisionY = centerY;
            result.normalX = -1;
            result.normalY = 0;
            result.penetrationDepth = radius;
            collided = true;
        }
        // 上边界碰撞
        else if (centerY - radius <= 0) {
            result.collisionX = centerX;
            result.collisionY = radius;
            result.normalX = 0;
            result.normalY = 1;
            result.penetrationDepth = radius;
            collided = true;
        }
        // 下边界碰撞
        else if (centerY + radius >= gameHeight) {
            result.collisionX = centerX;
            result.collisionY = gameHeight - radius;
            result.normalX = 0;
            result.normalY = -1;
            result.penetrationDepth = radius;
            collided = true;
        }
        
        // 如果检测到碰撞，处理反弹
        if (collided) {
            if (bullet.canBounce()) {
                if (bullet instanceof PlayerBullet) {
                    processBulletBounce((PlayerBullet)bullet, result);
                } else {
                    processEnemyBulletBounce((EnemyBullet)bullet, result);
                }
            } else {
                bullet.deactivate();
            }
            return true;
        }
        return false;
    }

    /**
     * 检查子弹轨迹与矩形段落的碰撞
     */
    private void checkSegmentCollision(
            BulletCollisionResult result, double sampleX, double sampleY, 
            double endX, double endY, double bulletRadius, 
            Rectangle segment, double moveX, double moveY) {
        
        // 扩展段落边界以考虑子弹半径
        Rectangle expandedSegment = new Rectangle(
            segment.x - (int)bulletRadius,
            segment.y - (int)bulletRadius,
            segment.width + (int)(bulletRadius * 2),
            segment.height + (int)(bulletRadius * 2)
        );
        
        // 计算子弹轨迹与扩展段落的交点
        double[] intersection = rayRectIntersection(
            sampleX, sampleY, endX, endY, expandedSegment);
        
        if (intersection[0] >= 0 && intersection[0] <= 1) {
            // 找到碰撞点
            double hitX = sampleX + intersection[0] * (endX - sampleX);
            double hitY = sampleY + intersection[0] * (endY - sampleY);
            
            // 计算距离
            double distance = intersection[0] * Math.sqrt(Math.pow(endX - sampleX, 2) + Math.pow(endY - sampleY, 2));
            
            // 如果这是最近的碰撞点，更新结果
            if (distance < result.distance) {
                result.hasCollided = true;
                result.distance = distance;
                result.collisionX = hitX;
                result.collisionY = hitY;
                result.collidedRect = segment;
                
                // 确定碰撞法线
                determineCollisionNormal(result, hitX, hitY, segment, moveX, moveY);
                
                // 计算穿透深度
                result.penetrationDepth = calculatePenetrationDepth(
                    hitX, hitY, segment, result.normalX, result.normalY);
            }
        }
    }

    /**
     * 确定碰撞法线
     */
    private void determineCollisionNormal(
            BulletCollisionResult result, double hitX, double hitY, 
            Rectangle wall, double moveX, double moveY) {
        
        // 计算子弹中心到墙体各边的距离
        double distLeft = Math.abs(hitX - wall.x);
        double distRight = Math.abs(hitX - (wall.x + wall.width));
        double distTop = Math.abs(hitY - wall.y);
        double distBottom = Math.abs(hitY - (wall.y + wall.height));
        
        // 找出最近的边
        double minDist = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));
        
        // 设置法线 - 始终指向远离墙体的方向
        if (minDist == distLeft) {
            result.normalX = -1;
            result.normalY = 0;
        } else if (minDist == distRight) {
            result.normalX = 1;
            result.normalY = 0;
        } else if (minDist == distTop) {
            result.normalX = 0;
            result.normalY = -1;
        } else {
            result.normalX = 0;
            result.normalY = 1;
        }
        
        // 处理超薄墙体的特殊情况
        if (wall.width < 10) {
            // 对于非常窄的墙，强制使用水平法线
            result.normalX = moveX > 0 ? -1 : 1;
            result.normalY = 0;
        } else if (wall.height < 10) {
            // 对于非常矮的墙，强制使用垂直法线
            result.normalX = 0;
            result.normalY = moveY > 0 ? -1 : 1;
        }
    }

    /**
     * 计算穿透深度
     */
    private double calculatePenetrationDepth(
            double hitX, double hitY, Rectangle wall, double normalX, double normalY) {
        
        // 计算子弹中心到墙体最近边的距离
        double distX, distY;
        
        // X方向距离
        if (hitX < wall.x) {
            distX = wall.x - hitX;
        } else if (hitX > wall.x + wall.width) {
            distX = hitX - (wall.x + wall.width);
        } else {
            distX = 0; // 子弹在墙体X范围内
        }
        
        // Y方向距离
        if (hitY < wall.y) {
            distY = wall.y - hitY;
        } else if (hitY > wall.y + wall.height) {
            distY = hitY - (wall.y + wall.height);
        } else {
            distY = 0; // 子弹在墙体Y范围内
        }
        
        // 如果法线是横向的
        if (Math.abs(normalX) > Math.abs(normalY)) {
            return distX > 0 ? distX : Math.min(wall.width, wall.height) * 0.5;
        } 
        // 如果法线是纵向的
        else {
            return distY > 0 ? distY : Math.min(wall.width, wall.height) * 0.5;
        }
    }

    /**
     * 处理玩家子弹反弹
     */
    private void processBulletBounce(PlayerBullet bullet, BulletCollisionResult collision) {
        // 获取子弹当前角度
        double angle = bullet.getAngle();
        
        // 计算入射向量
        double incidentX = Math.sin(angle);
        double incidentY = -Math.cos(angle);
        
        // 计算反射向量 R = I - 2(I·N)N
        double dot = incidentX * collision.normalX + incidentY * collision.normalY;
        double reflectX = incidentX - 2 * dot * collision.normalX;
        double reflectY = incidentY - 2 * dot * collision.normalY;
        
        // 计算新角度
        double newAngle = Math.atan2(reflectX, -reflectY);
        
        // 规范化角度到0-2π
        newAngle = (newAngle + 2 * Math.PI) % (2 * Math.PI);
        
        // 设置新角度
        bullet.setAngle(newAngle);
        
        // 计算子弹新位置 - 防止卡墙
        double safetyMultiplier;
        
        // 为边界碰撞提供额外的安全距离
        if (collision.isBoundary) {
            safetyMultiplier = 3.0; // 边界碰撞使用更大的安全距离
        } else {
            safetyMultiplier = 2.0; // 普通墙体碰撞
        }
        
        // 确保子弹完全脱离碰撞体
        double offsetX = collision.normalX * (collision.penetrationDepth * safetyMultiplier);
        double offsetY = collision.normalY * (collision.penetrationDepth * safetyMultiplier);
        
        // 如果是超薄墙体，增加额外偏移以防止穿墙
        if (collision.collidedRect != null) {
            if (collision.collidedRect.width < 10 || collision.collidedRect.height < 10) {
                offsetX *= 1.5;
                offsetY *= 1.5;
            }
        }
        
        // 设置子弹位置，确保不会卡在墙内
        bullet.setPosition((int)(collision.collisionX + offsetX), (int)(collision.collisionY + offsetY));
        
        // 减少速度模拟能量损失
        bullet.decreaseSpeed(0.9);
        
        // 增加微小的随机扰动，防止在某些特殊情况下的循环反弹
        if (Math.random() < 0.1) { // 10%的概率添加微小扰动
            double noise = (Math.random() - 0.5) * 0.05; // 很小的角度噪声
            newAngle = (newAngle + noise + 2 * Math.PI) % (2 * Math.PI);
            bullet.setAngle(newAngle);
        }
        
        // 增加反弹计数
        bullet.bounce();
    }

    /**
     * 处理敌方子弹反弹
     */
    private void processEnemyBulletBounce(EnemyBullet bullet, BulletCollisionResult collision) {
        // 获取子弹当前角度
        double angle = bullet.getAngle();
        
        // 注意:敌方子弹使用不同的方向计算方式
        // 计算入射向量
        double incidentX = Math.cos(angle);
        double incidentY = Math.sin(angle);
        
        // 计算反射向量 R = I - 2(I·N)N
        double dot = incidentX * collision.normalX + incidentY * collision.normalY;
        double reflectX = incidentX - 2 * dot * collision.normalX;
        double reflectY = incidentY - 2 * dot * collision.normalY;
        
        // 计算新角度
        double newAngle = Math.atan2(reflectY, reflectX);
        
        // 设置新角度
        bullet.setAngle(newAngle);
        
        // 计算子弹新位置 - 防止卡墙
        double safetyMultiplier;
        
        // 为边界碰撞提供额外的安全距离
        if (collision.isBoundary) {
            safetyMultiplier = 3.0; // 边界碰撞使用更大的安全距离
        } else {
            safetyMultiplier = 2.0; // 普通墙体碰撞
        }
        
        // 确保子弹完全脱离碰撞体
        double offsetX = collision.normalX * (collision.penetrationDepth * safetyMultiplier);
        double offsetY = collision.normalY * (collision.penetrationDepth * safetyMultiplier);
        
        // 如果是超薄墙体，增加额外偏移以防止穿墙
        if (collision.collidedRect != null) {
            if (collision.collidedRect.width < 10 || collision.collidedRect.height < 10) {
                offsetX *= 1.5;
                offsetY *= 1.5;
            }
        }
        
        // 设置子弹位置，确保不会卡在墙内
        bullet.setPosition((int)(collision.collisionX + offsetX), (int)(collision.collisionY + offsetY));
        
        // 减少速度模拟能量损失
        bullet.decreaseSpeed(0.92);
        
        // 增加微小的随机扰动，防止在某些特殊情况下的循环反弹
        if (Math.random() < 0.15) { // 15%的概率添加微小扰动
            double noise = (Math.random() - 0.5) * 0.08; // 很小的角度噪声
            newAngle = (newAngle + noise + 2 * Math.PI) % (2 * Math.PI);
            bullet.setAngle(newAngle);
        }
        
        // 增加反弹计数
        bullet.bounce();
    }

    /**
     * 检查与游戏边界的碰撞
     */
    private void checkBoundaryCollision(
            BulletCollisionResult result, double startX, double startY, 
            double endX, double endY, double bulletRadius) {
        
        int gameWidth = getWidth();
        int gameHeight = getHeight();
        
        // 增加边界安全偏移量
        double safetyOffset = 2.0;
        double adjustedRadius = bulletRadius + safetyOffset;
        
        // 左边界
        if (endX - adjustedRadius < 0) {
            double t = (adjustedRadius - startX) / (endX - startX);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = adjustedRadius; // 确保碰撞点在边界外
                result.collisionY = startY + t * (endY - startY);
                result.normalX = 1;
                result.normalY = 0;
                result.penetrationDepth = adjustedRadius;
                result.isBoundary = true; // 标记为边界碰撞
            }
        }
        
        // 右边界
        if (endX + adjustedRadius > gameWidth) {
            double t = (gameWidth - adjustedRadius - startX) / (endX - startX);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = gameWidth - adjustedRadius; // 确保碰撞点在边界外
                result.collisionY = startY + t * (endY - startY);
                result.normalX = -1;
                result.normalY = 0;
                result.penetrationDepth = adjustedRadius;
                result.isBoundary = true; // 标记为边界碰撞
            }
        }
        
        // 上边界
        if (endY - adjustedRadius < 0) {
            double t = (adjustedRadius - startY) / (endY - startY);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = startX + t * (endX - startX);
                result.collisionY = adjustedRadius; // 确保碰撞点在边界外
                result.normalX = 0;
                result.normalY = 1;
                result.penetrationDepth = adjustedRadius;
                result.isBoundary = true; // 标记为边界碰撞
            }
        }
        
        // 下边界
        if (endY + adjustedRadius > gameHeight) {
            double t = (gameHeight - adjustedRadius - startY) / (endY - startY);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = startX + t * (endX - startX);
                result.collisionY = gameHeight - adjustedRadius; // 确保碰撞点在边界外
                result.normalX = 0;
                result.normalY = -1;
                result.penetrationDepth = adjustedRadius;
                result.isBoundary = true; // 标记为边界碰撞
            }
        }
    }

    /**
     * 计算射线与矩形的交点
     */
    private double[] rayRectIntersection(double startX, double startY, double endX, double endY, Rectangle rect) {
        double[] result = {Double.MAX_VALUE, -1}; // t值和碰撞面索引
        
        // 射线方向向量
        double dirX = endX - startX;
        double dirY = endY - startY;
        
        // 矩形的四条边
        double[] tValues = new double[4];
        int[] sides = new int[4];
        
        // 左边 (x = rect.x)
        if (dirX != 0) {
            double t = (rect.x - startX) / dirX;
            double y = startY + t * dirY;
            if (y >= rect.y && y <= rect.y + rect.height) {
                tValues[0] = t;
                sides[0] = 0;
            } else {
                tValues[0] = Double.MAX_VALUE;
            }
        } else {
            tValues[0] = Double.MAX_VALUE;
        }
        
        // 右边 (x = rect.x + rect.width)
        if (dirX != 0) {
            double t = (rect.x + rect.width - startX) / dirX;
            double y = startY + t * dirY;
            if (y >= rect.y && y <= rect.y + rect.height) {
                tValues[1] = t;
                sides[1] = 1;
            } else {
                tValues[1] = Double.MAX_VALUE;
            }
        } else {
            tValues[1] = Double.MAX_VALUE;
        }
        
        // 上边 (y = rect.y)
        if (dirY != 0) {
            double t = (rect.y - startY) / dirY;
            double x = startX + t * dirX;
            if (x >= rect.x && x <= rect.x + rect.width) {
                tValues[2] = t;
                sides[2] = 2;
            } else {
                tValues[2] = Double.MAX_VALUE;
            }
        } else {
            tValues[2] = Double.MAX_VALUE;
        }
        
        // 下边 (y = rect.y + rect.height)
        if (dirY != 0) {
            double t = (rect.y + rect.height - startY) / dirY;
            double x = startX + t * dirX;
            if (x >= rect.x && x <= rect.x + rect.width) {
                tValues[3] = t;
                sides[3] = 3;
            } else {
                tValues[3] = Double.MAX_VALUE;
            }
        } else {
            tValues[3] = Double.MAX_VALUE;
        }
        
        // 找到最小的非负t值
        for (int i = 0; i < 4; i++) {
            if (tValues[i] >= 0 && tValues[i] < result[0]) {
                result[0] = tValues[i];
                result[1] = sides[i];
            }
        }
        
        return result;
    }

    /**
     * 增强的子弹碰撞结果类
     */
    private static class BulletCollisionResult {
        boolean hasCollided = false;       // 是否发生碰撞
        double collisionX = 0;             // 碰撞点X坐标
        double collisionY = 0;             // 碰撞点Y坐标
        double normalX = 0;                // 碰撞表面法线X分量
        double normalY = 0;                // 碰撞表面法线Y分量
        double penetrationDepth = 0;       // 穿透深度
        double distance = Double.MAX_VALUE; // 到碰撞点的距离
        Rectangle collidedRect = null;     // 碰撞的矩形
        boolean isBoundary = false;        // 是否是边界碰撞
    }
    // 处理子弹反弹的辅助方法
    private void handleBulletBounce(PlayerBullet bullet, boolean isHorizontalCollision) {
        double angle = bullet.getAngle();

        if (isHorizontalCollision) {
            // 水平反弹
            bullet.setAngle(Math.PI - angle);
            // 反向移动一小段距离避免穿模
            double dx = Math.sin(angle);
            bullet.adjustPosition(-(int)(dx * 5), 0);
        } else {
            // 垂直反弹
            bullet.setAngle(-angle);
            // 反向移动一小段距离避免穿模
            double dy = -Math.cos(angle);
            bullet.adjustPosition(0, (int)(dy * 5));
        }

        // 增加反弹计数
        bullet.bounce();
    }
    // 处理敌方子弹反弹
    private void handleEnemyBulletBounce(EnemyBullet bullet, boolean isHorizontalCollision) {
        double angle = bullet.getAngle();

        if (isHorizontalCollision) {
            // 水平反弹
            bullet.setAngle(Math.PI - angle);
            // 反向移动一小段距离避免穿模
            double dx = Math.sin(angle);
            bullet.adjustPosition(-(int)(dx * 5), 0);
        } else {
            // 垂直反弹
            bullet.setAngle(-angle);
            // 反向移动一小段距离避免穿模
            double dy = -Math.cos(angle);
            bullet.adjustPosition(0, (int)(dy * 5));
        }

        // 增加反弹计数
        bullet.bounce();
    }

    private void checkBulletCollisions() {
        if (player == null || enemies.isEmpty() || player.getBullets() == null) {
            return;
        }

        // 检查玩家子弹是否击中敌方坦克
        for (PlayerBullet bullet : player.getBullets()) {
            if (bullet != null && bullet.isActive()) {
                Rectangle bulletBounds = bullet.getCollisionBounds();
                if (bulletBounds == null) continue;

                for (int i = 0; i < enemies.size(); i++) {
                    EnemyTank enemy = enemies.get(i);
                    if (enemy.isAlive()) {
                        Rectangle enemyBounds = enemy.getCollisionBounds();

                        if (enemyBounds != null && bulletBounds.intersects(enemyBounds)) {
                            bullet.deactivate(); // 击中坦克直接消失，不反弹
                            // 在坦克死亡前，保存它的子弹到孤儿子弹列表
                            List<EnemyBullet> activeBullets = new ArrayList<>(enemy.getBullets());
                            orphanedBullets.addAll(activeBullets);
                            // 清空坦克的子弹列表，防止重复
                            enemy.getBullets().clear();
                            // 然后再处理坦克伤害
                            enemy.takeDamage(bullet.getDamage());
                            if (!enemy.isAlive()) {
                                // 敌方坦克被摧毁，增加得分
                                ConfigTool.setBeatNum(String.valueOf(ConfigTool.getBeatNum() + 1));
                                ConfigTool.saveConfig();
                                updateDisplays();
                                // 移除被摧毁的敌方坦克
                                enemies.remove(i);
                                // 如果没有敌人了，随机重生1-3个敌人
                                if (enemies.isEmpty()) {
                                    Timer respawnTimer = new Timer(1000, e -> {
                                        respawnEnemies();
                                        ((Timer)e.getSource()).stop();
                                    });
                                    respawnTimer.setRepeats(false);
                                    respawnTimer.start();
                                }
                            }
                            break; // 子弹已经命中，不再继续检查
                        }
                    }
                }
            }
        }

        // 检查敌方子弹是否击中玩家坦克
        for (EnemyTank enemy : enemies) {
            for (EnemyBullet bullet : enemy.getBullets()) {
                if (bullet.isActive() && player.isAlive()) {
                    Rectangle bulletBounds = bullet.getCollisionBounds();
                    Rectangle playerBounds = player.getCollisionBounds();

                    if (bulletBounds != null && playerBounds != null &&
                            bulletBounds.intersects(playerBounds)) {
                        // 击中玩家坦克
                        bullet.deactivate();
                        player.takeDamage(bullet.getDamage());
                        updateDisplays();
                        
                        // 检查游戏是否结束,使用明确的生命值检查
                        if (player.getHealth() <= 0) {
                            System.out.println("玩家生命值为0，游戏结束");
                            gameOver();
                        }
                    }
                }
            }
        }
    }
    private void respawnEnemies() {
        Random rand = new Random();
        int enemyCount = rand.nextInt(3) + 1; // 1到3之间的随机数

        for (int i = 0; i < enemyCount; i++) {
            createEnemy();
        }
    }

    // 游戏结束处理
    private void gameOver() {
        gameRunning = false;
        gameTimer.stop();
        
        // 清除所有爆炸效果
        ExplosionManager.getInstance().clearAllExplosions();
        
        // 显示游戏结束对话框，并在用户点击确定后返回主界面
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "游戏结束！\n击败敌方坦克数: " + ConfigTool.getBeatNum(),
                    "游戏结束",
                    JOptionPane.INFORMATION_MESSAGE);

            endGame();
            
            // 查找所属的CardLayout和主面板
            Container parent = getParent();
            while (parent != null && !(parent.getLayout() instanceof CardLayout)) {
                parent = parent.getParent();
            }
            
            if (parent != null) {
                CardLayout cardLayout = (CardLayout) parent.getLayout();
                cardLayout.show(parent, "Menu");
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 绘制墙体
        for (PVPWall wall : PVPWalls) {
            wall.draw(g);
        }
        
        // 在倒计时或游戏运行时都绘制玩家坦克
        if ((gameRunning || isCountingDown) && player != null) {
            player.draw(g);
        }

        // 在倒计时或游戏运行时都绘制敌方坦克
        if (gameRunning || isCountingDown) {
            // 绘制所有敌方坦克
            for (EnemyTank enemy : enemies) {
                enemy.draw(g);
            }
            
            // 绘制孤儿子弹
            for (EnemyBullet bullet : orphanedBullets) {
                bullet.draw(g);
            }
        }
        
        // 最后绘制爆炸效果（最高优先级）
        ExplosionManager.getInstance().draw(g);
        
        // 绘制倒计时
        if (isCountingDown) {
            drawCountDown(g);
        }
    }

    /**
     * 绘制倒计时数字
     */
    private void drawCountDown(Graphics g) {
        // 保存原始颜色
        Color originalColor = g.getColor();
        
        // 设置倒计时文本属性
        int fontSize = Math.min(getWidth(), getHeight()) / 5; // 根据游戏区域大小调整字体
        Font countdownFont = new Font("Arial", Font.BOLD, fontSize);
        g.setFont(countdownFont);
        
        // 计算文本绘制位置（居中）
        String countdownText = String.valueOf(countDownSeconds);
        FontMetrics fm = g.getFontMetrics(countdownFont);
        int textWidth = fm.stringWidth(countdownText);
        int textHeight = fm.getHeight();
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - textHeight) / 2 + fm.getAscent();
        
        // 添加文本阴影效果增强可见性
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(countdownText, x + 5, y + 5);
        
        // 根据剩余时间变化颜色
        Color textColor;
        if (countDownSeconds > 2) {
            textColor = new Color(0, 200, 0); // 绿色
        } else if (countDownSeconds > 1) {
            textColor = new Color(255, 165, 0); // 橙色
        } else {
            textColor = new Color(255, 0, 0); // 红色
        }
        g.setColor(textColor);
        
        // 创建淡入淡出效果
        long elapsedTime = System.currentTimeMillis() - countDownStartTime;
        int fadeTime = 1000; // 每秒的淡入淡出时间
        int currentSecondElapsed = (int)(elapsedTime % fadeTime);
        float alpha = 1.0f;
        
        // 在每秒的后半段逐渐淡出
        if (currentSecondElapsed > fadeTime / 2) {
            alpha = 1.0f - (currentSecondElapsed - fadeTime / 2) / (float)(fadeTime / 2);
        }
        
        // 应用透明度
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
        }
        
        // 绘制倒计时数字
        g.drawString(countdownText, x, y);
        
        // 恢复原始设置
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
        }
        g.setColor(originalColor);
    }

    public void startGame() {
        // 重置游戏统计数据
        ConfigTool.resetGameStats(); // 重置击败数为0
        updateDisplays(); // 更新显示
        
        // 确保玩家坦克在正确位置
        repositionPlayerTank();
        
        // 如果敌人列表为空，创建初始敌人
        if (enemies.isEmpty()) {
            createInitialEnemies();
        }
        
        // 开始倒计时而不是直接启动游戏
        startCountDown();
    }

    /**
     * 开始倒计时
     */
    private void startCountDown() {
        // 设置倒计时状态
        isCountingDown = true;
        countDownSeconds = 3;
        countDownStartTime = System.currentTimeMillis();
        
        // 确保游戏不会在倒计时期间运行
        gameRunning = false;
        
        // 请求焦点，使键盘输入有效
        requestFocus();
        
        // 创建倒计时定时器，每秒触发一次
        if (countDownTimer != null) {
            countDownTimer.stop();
        }
        
        countDownTimer = new Timer(1000, e -> {
            countDownSeconds--;
            repaint(); // 刷新显示
            
            if (countDownSeconds <= 0) {
                // 倒计时结束，停止定时器
                ((Timer)e.getSource()).stop();
                // 真正启动游戏
                finalizeGameStart();
                isCountingDown = false;
            }
        });
        
        countDownTimer.setRepeats(true);
        countDownTimer.start();
        repaint(); // 立即刷新显示第一个数字
    }

    /**
     * 倒计时结束后真正启动游戏
     */
    private void finalizeGameStart() {
        gameRunning = true;
        gameTimer.start();
        System.out.println("倒计时结束，游戏开始!");
    }

    public void stopGame() {
        gameRunning = false;
    }

    public void endGame() {
        gameRunning = false;
        gameTimer.stop();
        enemies.clear();
        
        // 清除所有爆炸效果
        ExplosionManager.getInstance().clearAllExplosions();
    }

    public void resetGame() {
        // 清空敌人
        enemies.clear();
        
        // 重置玩家坦克,使用新的坦克实例完全重置
        player = new PlayerTank(50, 50, detector);
        PlayerTank.resetHealth(); // 确保静态生命值被重置
        
        // 重置游戏状态
        gameRunning = false;
        
        // 不立即重定位玩家坦克，等游戏开始时再定位
        // 仅创建敌方坦克但不显示（由于gameRunning=false）
        createInitialEnemies();
        
        // 清空孤儿子弹列表
        orphanedBullets.clear();
        
        // 清除所有爆炸效果
        ExplosionManager.getInstance().clearAllExplosions();

        // 重置游戏统计数据
        ConfigTool.resetGameStats();
        updateDisplays();
    }

    public CollisionDetector getDetector() {
        return detector;
    }

}