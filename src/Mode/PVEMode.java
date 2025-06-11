// PVEMode.java
package src.Mode;

import src.Config.*;
import src.InterFace.CollisionDetector;
import src.Structure.PVEWall;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PVEMode extends JPanel implements KeyListener {
    // 游戏区域尺寸
    private int gameAreaWidth;
    private int gameAreaHeight;
    
    // 游戏实体
    private PlayerTank player;
    private AITank aiTank;
    private ArrayList<PVEWall> walls;
    
    // 游戏控制
    private Timer gameTimer;
    private boolean gameRunning;
    private CollisionDetector detector;
    
    // UI元素
    private JLabel levelLabel;
    private JLabel scoreLabel;
    
    // 游戏状态
    private int currentLevel;
    private int playerScore;
    private int enemyScore;
    
    // 游戏常量
    private static final int SCORE_TO_WIN = 3;
    private static final int SCORE_TO_LOSE = 10;
    
    // 调试标记
    private boolean debugMode = false;

    // 添加暂停状态标志
    private boolean isPaused = false;

    // 添加以下成员变量到类顶部
    private boolean isCountingDown = false;
    private int countDownSeconds = 3;
    private Timer countDownTimer;
    private long countDownStartTime;

    /**
     * 构造函数
     */
    public PVEMode(CollisionDetector detector, JLabel levelLabel, JLabel scoreLabel) {
        this.detector = detector;
        this.levelLabel = levelLabel;
        this.scoreLabel = scoreLabel;
        this.currentLevel = ConfigTool.getLevel();
        this.playerScore = ConfigTool.getOurScore();
        this.enemyScore = ConfigTool.getEnemyScore();

        // 面板设置
        setLayout(null);
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // 组件大小变化监听
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 更新游戏区域尺寸
                gameAreaWidth = getWidth();
                gameAreaHeight = getHeight();

                // 更新碰撞检测器的游戏区域大小
                if (detector instanceof SimpleCollisionDetector) {
                    ((SimpleCollisionDetector)detector).setGameAreaSize(
                            new Dimension(gameAreaWidth, gameAreaHeight)
                    );
                }

                // 确保坦克在新边界内
                if (player != null && aiTank != null) {
                    checkAndAdjustTankPositions();
                }
                
                // 当窗口大小变化时重新初始化墙体
                if (gameRunning && walls != null) {
                    initWalls();
                }
            }
        });

        // 初始化游戏组件
        initGame();

        // 创建游戏循环定时器 (60 FPS)
        gameTimer = new Timer(16, e -> {
            if (gameRunning) {
                updateGame();
                repaint();
            }
        });
    }

    /**
     * 初始化游戏组件
     */
    private void initGame() {
        walls = new ArrayList<>();
        
        // 创建玩家坦克但位置在屏幕外（隐藏）
        player = new PlayerTank(-100, -100, detector);
        
        // 检查游戏区域尺寸是否有效
        gameAreaWidth = getWidth();
        gameAreaHeight = getHeight();
        
        // 如果尺寸无效（窗口尚未布局完成），使用默认值
        if (gameAreaWidth <= 0) gameAreaWidth = 800;
        if (gameAreaHeight <= 0) gameAreaHeight = 600;
        
        // 更新碰撞检测器中的游戏区域大小
        if (detector instanceof SimpleCollisionDetector) {
            ((SimpleCollisionDetector)detector).setGameAreaSize(
                    new Dimension(gameAreaWidth, gameAreaHeight)
            );
        }
        
        // 只在第一次初始化AI坦克
        if (aiTank == null) {
            spawnAITank();
        } else {
            // 已存在则只重置状态和位置
            resetAITank();
        }
        
        // 更新UI显示
        updateDisplays();
    }

    /**
     * 重置AI坦克位置（保留学习数据）
     */
    private void resetAITank() {
        if (aiTank != null) {
            // 生成新的随机位置
            Random rand = new Random();
            int margin = 100;
            int x, y;
            int attempts = 0;
            final int MAX_ATTEMPTS = 50;
            
            // 默认位置（如果找不到合适位置）
            x = gameAreaWidth - margin - 64;
            y = gameAreaHeight / 2 - 32;

            // 尝试找到不与墙体碰撞的位置
            boolean positionFound = false;
            while (attempts < MAX_ATTEMPTS && !positionFound) {
                int newX = margin + rand.nextInt(Math.max(1, gameAreaWidth - 2 * margin - 64));
                int newY = margin + rand.nextInt(Math.max(1, gameAreaHeight - 2 * margin - 64));
                
                // 只检查墙体碰撞
                if (!isPositionBlockedByWalls(newX, newY)) {
                    x = newX;
                    y = newY;
                    positionFound = true;
                }
                attempts++;
            }

            // 更新位置和状态，保持AI学习数据
            aiTank.setPosition(x, y);
            aiTank.revive();
        }
    }

    /**
     * 初始化墙体
     */
    private void initWalls() {
        walls.clear(); // 清除已有墙体

        // 获取游戏区域大小 - 确保使用正确的尺寸
        int areaWidth = getWidth();
        int areaHeight = getHeight();
        
        // 如果尺寸无效，使用默认值
        if (areaWidth <= 0) areaWidth = 800;
        if (areaHeight <= 0) areaHeight = 600;
        
        // 创建玩家和AI坦克的碰撞边界
        Rectangle playerBounds = player != null ?
                new Rectangle(player.getX(), player.getY(), player.getWidth(), player.getHeight()) :
                new Rectangle(50, 50, 64, 64);

        Rectangle aiBounds = aiTank != null && aiTank.isAlive() ?
                new Rectangle(aiTank.getX(), aiTank.getY(), aiTank.getWidth(), aiTank.getHeight()) :
                null;

        // 生成墙体
        PVEWall[] generatedWalls = PVEWall.generateWalls(areaWidth, areaHeight, playerBounds, aiBounds);

        // 添加墙体到列表
        Collections.addAll(walls, generatedWalls);

        // 更新碰撞检测器
        updateCollisionDetector();
    }

    private void updateCollisionDetector() {
        if (detector instanceof SimpleCollisionDetector) {
            SimpleCollisionDetector simpleDetector = (SimpleCollisionDetector) detector;
            // 更新游戏区域大小
            simpleDetector.setGameAreaSize(new Dimension(gameAreaWidth, gameAreaHeight));
            // 更新PVEWall信息
            simpleDetector.setPVEWalls(walls);
        }
    }

    /**
     * 确保坦克在游戏区域内
     */
    private void checkAndAdjustTankPositions() {
        // 调整玩家坦克位置
        if (player != null) {
            int x = Math.min(Math.max(player.getX(), 0), gameAreaWidth - player.getWidth());
            int y = Math.min(Math.max(player.getY(), 0), gameAreaHeight - player.getHeight());
            
            // 只有当坦克真的超出边界时才移动它
            if (x != player.getX() || y != player.getY()) {
                player.setPosition(x, y);
            }
        }

        // 调整AI坦克位置
        if (aiTank != null) {
            int x = Math.min(Math.max(aiTank.getX(), 0), gameAreaWidth - aiTank.getWidth());
            int y = Math.min(Math.max(aiTank.getY(), 0), gameAreaHeight - aiTank.getHeight());
            
            // 只有当坦克真的超出边界时才移动它
            if (x != aiTank.getX() || y != aiTank.getY()) {
                aiTank.setPosition(x, y);
            }
        }
    }

    /**
     * 生成AI坦克
     */
    private void spawnAITank() {
        Random rand = new Random();
        int margin = 100;
        int x, y;
        
        // 默认位置 - 在右侧中间
        x = gameAreaWidth - margin - 64;
        y = gameAreaHeight / 2 - 32;
        
        int attempts = 0;
        final int MAX_ATTEMPTS = 50;
        
        boolean positionFound = false;
        while (attempts < MAX_ATTEMPTS && !positionFound) {
            int newX = margin + rand.nextInt(Math.max(1, gameAreaWidth - 2 * margin - 64));
            int newY = margin + rand.nextInt(Math.max(1, gameAreaHeight - 2 * margin - 64));
            
            // 只检查墙体碰撞
            if (!isPositionBlockedByWalls(newX, newY)) {
                x = newX;
                y = newY;
                positionFound = true;
            }
            attempts++;
        }
        
        if (aiTank == null) {
            aiTank = new AITank(x, y, detector);
        } else {
            aiTank.setPosition(x, y);
            aiTank.revive();
        }
    }
    
    
    /**
     * 检查位置是否被墙体阻挡
     */
    private boolean isPositionBlockedByWalls(int x, int y) {
        // 检查边界
        if (x < 0 || y < 0 || x + 64 > gameAreaWidth || y + 64 > gameAreaHeight) {
            return true;
        }

        Rectangle newPos = new Rectangle(x, y, 64, 64);

        // 检查与墙体的碰撞
        for (PVEWall wall : walls) {
            if (wall.getCollisionBounds().intersects(newPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 重生坦克
     */
    private void respawnTank(AbstractTank tank, AbstractTank other) {
        Random rand = new Random();
        int margin = 50;
        int x, y;
        
        // 设置默认重生位置
        if (tank instanceof PlayerTank) {
            // 玩家默认重生在左侧
            x = margin;
            y = gameAreaHeight / 2;
        } else {
            // AI默认重生在右侧
            x = gameAreaWidth - margin - tank.getWidth();
            y = gameAreaHeight / 2;
        }
        
        int attempts = 0;
        final int MAX_ATTEMPTS = 50;
        
        // 尝试找到不与墙体碰撞的随机位置
        boolean positionFound = false;
        while (attempts < MAX_ATTEMPTS && !positionFound) {
            int newX = margin + rand.nextInt(gameAreaWidth - 2 * margin - tank.getWidth());
            int newY = margin + rand.nextInt(gameAreaHeight - 2 * margin - tank.getHeight());
            
            // 只检查墙体碰撞，完全忽略其他坦克
            if (!isPositionBlockedByWalls(newX, newY)) {
                x = newX;
                y = newY;
                positionFound = true;
            }
            attempts++;
        }
        
        // 设置位置并复活
        tank.setPosition(x, y);
        tank.revive();
        
        // 如果是玩家坦克，重置其键盘状态
        if (tank instanceof PlayerTank) {
            ((PlayerTank)tank).resetKeyStates();
        }
    }

    /**
     * 更新游戏状态
     */
    private void updateGame() {
        // 更新玩家坦克移动 - 只在存活时更新移动
        if (player.isAlive()) {
            player.updateMovement();
        }
        // 无论玩家是否存活，都更新子弹
        player.updateBullets();
        
        // 更新AI坦克
        if (aiTank != null && aiTank.isAlive()) {
            aiTank.updateAI(player, currentLevel);
        }
        // 无论AI是否存活，都更新其子弹
        if (aiTank != null) {
            aiTank.updateBullets();
        }
        
        // 检测碰撞
        checkCollisions();
        checkBulletWallCollisions();
        
        // 检查得分情况
        checkScores();
        updateDisplays();

        // 更新爆炸效果
        ExplosionManager.getInstance().update();
    }
    
    /**
     * 优化的子弹与墙体碰撞检测
     */
    private void checkBulletWallCollisions() {
        // 玩家子弹检测逻辑保持不变...
        
        // 检测AI子弹与墙体碰撞 - 优化版
        if (aiTank != null) {
            for (EnemyBullet bullet : aiTank.getBullets()) {
                if (!bullet.isActive()) continue;

                Rectangle bulletBounds = bullet.getCollisionBounds();
                if (bulletBounds == null) continue;
                
                // 获取子弹中心点
                int bulletCenterX = bulletBounds.x + bulletBounds.width / 2;
                int bulletCenterY = bulletBounds.y + bulletBounds.height / 2;
                
                // 即时边界检查 - 防止子弹已经越过边界
                int gameWidth = getWidth();
                int gameHeight = getHeight();
                int radius = bullet.getRadius();
                
                if (bulletCenterX - radius <= 0 || 
                    bulletCenterX + radius >= gameWidth || 
                    bulletCenterY - radius <= 0 || 
                    bulletCenterY + radius >= gameHeight) {
                    
                    // 创建边界碰撞结果
                    BulletCollisionResult collision = new BulletCollisionResult();
                    collision.hasCollided = true;
                    collision.isBoundaryCollision = true;
                    
                    // 确定碰撞边界和法线
                    if (bulletCenterX - radius <= 0) {
                        collision.collisionX = radius;
                        collision.collisionY = bulletCenterY;
                        collision.normalX = 1;
                        collision.normalY = 0;
                    } else if (bulletCenterX + radius >= gameWidth) {
                        collision.collisionX = gameWidth - radius;
                        collision.collisionY = bulletCenterY;
                        collision.normalX = -1;
                        collision.normalY = 0;
                    } else if (bulletCenterY - radius <= 0) {
                        collision.collisionX = bulletCenterX;
                        collision.collisionY = radius;
                        collision.normalX = 0;
                        collision.normalY = 1;
                    } else {
                        collision.collisionX = bulletCenterX;
                        collision.collisionY = gameHeight - radius;
                        collision.normalX = 0;
                        collision.normalY = -1;
                    }
                    
                    collision.penetrationDepth = radius * 1.5;
                    
                    // 处理边界碰撞
                    if (bullet.canBounce()) {
                        processAIBulletBounce(bullet, collision);
                    } else {
                        bullet.deactivate();
                    }
                    
                    continue; // 已处理边界碰撞，跳过后续检测
                }
                
                // 获取子弹方向和速度
                double angle = bullet.getAngle();
                double speed = bullet.getSpeed();
                double dx = Math.cos(angle);
                double dy = Math.sin(angle);
                
                // 预测下一位置 - 使用增强预测系数
                double predictionFactor = Math.min(1.5, 1 + speed / 20.0);
                int nextX = (int)(bulletCenterX + dx * speed * predictionFactor);
                int nextY = (int)(bulletCenterY + dy * speed * predictionFactor);
                
                // 使用优化的碰撞检测
                BulletCollisionResult collision = detectAIBulletCollision(
                    bulletCenterX, bulletCenterY, nextX, nextY, bullet.getRadius());
                
                // 处理碰撞结果
                if (collision.hasCollided && bullet.canBounce()) {
                    processAIBulletBounce(bullet, collision);
                }
            }
        }
    }

    /**
     * 检查碰撞
     */
    private void checkCollisions() {
        // 玩家子弹击中AI
        if (player.isAlive() && aiTank != null && aiTank.isAlive()) {
            for (PlayerBullet bullet : player.getBullets()) {
                if (!bullet.isActive()) continue;
                if (bullet.getCollisionBounds().intersects(aiTank.getCollisionBounds())) {
                    // 击中AI坦克
                    bullet.deactivate();
                    
                    // 在击中位置创建爆炸效果 - 仅在击中坦克时才创建爆炸
                    createExplosion(aiTank);
                    
                    // 设置AI为死亡状态
                    aiTank.setAlive(false);
                    
                    // 在AI坦克死亡时调用学习方法
                    aiTank.onDeath(player);
                    
                    // 更新分数
                    playerScore++;
                    ConfigTool.setOurScore(String.valueOf(playerScore));
                    
                    // 延迟重生AI坦克
                    Timer respawnTimer = new Timer(800, e -> respawnTank(aiTank, player));
                    respawnTimer.setRepeats(false);
                    respawnTimer.start();
                    break;
                }
            }
        }
        
        // AI子弹击中玩家
        if (aiTank != null && aiTank.isAlive() && player.isAlive()) {
            for (EnemyBullet bullet : aiTank.getBullets()) {
                if (!bullet.isActive()) continue;
                
                // 简化判断条件，子弹已飞行足够距离且不是刚生成时就在坦克内部
                // Skip if bullet is too close to its origin to prevent immediate self-collision
                if (!bullet.isActive()) {
                    continue;
                }
                
                // 检查碰撞
                Rectangle bulletBounds = bullet.getCollisionBounds();
                Rectangle playerBounds = player.getCollisionBounds();
                
                if (bulletBounds != null && playerBounds != null && 
                    bulletBounds.intersects(playerBounds)) {
                    
                    // 子弹击中玩家，创建爆炸效果 - 仅在击中坦克时才创建爆炸
                    bullet.deactivate();
                    createExplosion(player);
                    
                    // 调用AI的registerHit方法记录命中
                    aiTank.registerHit();
                    
                    // 设置玩家死亡并重置键盘状态
                    player.setAlive(false);
                    player.resetKeyStates();
                    
                    // 更新分数
                    enemyScore++;
                    ConfigTool.setEnemyScore(String.valueOf(enemyScore));
                    
                    // 检查游戏结束条件
                    if (enemyScore >= SCORE_TO_LOSE) {
                        gameOver();
                        return;
                    }
                    
                    // 延迟重生玩家
                    Timer respawnTimer = new Timer(800, e -> respawnTank(player, aiTank));
                    respawnTimer.setRepeats(false);
                    respawnTimer.start();

                    break;
                }
            }
        }
    }
    
    /**
     * 为坦克创建爆炸效果
     */
    private void createExplosion(AbstractTank tank) {
        // 获取坦克中心点
        int centerX = tank.getX() + tank.getWidth() / 2;
        int centerY = tank.getY() + tank.getHeight() / 2;
        
        // 爆炸尺寸为坦克尺寸的1.5倍
        int explosionSize = (int)(Math.max(tank.getWidth(), tank.getHeight()) * 1.5);
        
        // 使用ExplosionManager创建爆炸效果
        ExplosionManager.getInstance().createExplosion(centerX, centerY, explosionSize);

    }

    /**
     * 检查得分情况
     */
    private void checkScores() {
        if (playerScore >= SCORE_TO_WIN) {
            advanceToNextLevel();
        } else if (enemyScore >= SCORE_TO_LOSE) {
            gameOver();
        }
    }

    /**
     * 游戏结束处理
     */
    private void gameOver() {
        // 确保只执行一次
        if (!gameRunning) return;
        
        gameRunning = false;
        gameTimer.stop();
        
        // 清除所有爆炸效果
        ExplosionManager.getInstance().clearAllExplosions();
        
        // 先保存AI学习数据
        if (aiTank != null) {
            aiTank.saveLearnedData();
        }
        
        // 重置游戏数据
        ConfigTool.setLevel("1");
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");
        
        // 显示游戏结束消息
        JOptionPane.showMessageDialog(this, 
            "游戏结束！\n止步于第 " + currentLevel + " 关\n我方得分: " + playerScore + "\n敌方得分: " + enemyScore,
            "游戏结束", JOptionPane.INFORMATION_MESSAGE);

        // 返回主菜单
        SwingUtilities.invokeLater(() -> {
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

    /**
     * 进入下一关
     */
    private void advanceToNextLevel() {
        currentLevel++;
        ConfigTool.setLevel(String.valueOf(currentLevel));
        playerScore = 0;
        enemyScore = 0;
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");

        // 重置玩家坦克
        player = new PlayerTank(50, 50, detector);
        
        // 清除墙体并重新生成
        walls = new ArrayList<>();
        initWalls();

        // 重置AI坦克位置，保持AI学习数据
        if (aiTank != null) {
            resetAITank();
        }

        updateDisplays();
        gameRunning = true;
    }

    /**
     * 更新UI显示
     */
    private void updateDisplays() {
        if (levelLabel != null) {
            levelLabel.setText("<html><div style='text-align: center;'>第<br>"
                    + currentLevel + "<br>关</div></html>");
        }
        
        if (scoreLabel != null) {
            scoreLabel.setText("<html><div style='text-align: center;'>我方<br>"
                    + playerScore + ":" + enemyScore + "<br>敌方</div></html>");
        }
    }

    /**
     * 绘制游戏组件
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 设置高质量渲染
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        
        // 只在游戏运行或倒计时时绘制墙体
        if ((gameRunning || isCountingDown) && walls != null) {
            for (PVEWall wall : walls) {
                wall.draw(g);
            }
        }
        
        // 只有在游戏运行或倒计时时绘制
        if (gameRunning || isCountingDown) {
            // 绘制玩家坦克（如果存活）
            if (player != null && player.isAlive()) {
                player.draw(g);
            }
            
            // 绘制AI坦克（如果存活）
            if (aiTank != null && aiTank.isAlive()) {
                aiTank.draw(g);
            }
            
            // 单独绘制玩家子弹 - 无论坦克是否存活
            if (player != null) {
                for (PlayerBullet bullet : player.getBullets()) {
                    if (bullet.isActive()) {
                        bullet.draw(g);
                    }
                }
            }
            
            // 单独绘制AI子弹 - 无论坦克是否存活
            if (aiTank != null) {
                for (EnemyBullet bullet : aiTank.getBullets()) {
                    if (bullet.isActive()) {
                        bullet.draw(g);
                    }
                }
            }
        }
        
        // 最后绘制爆炸效果（最高优先级）
        ExplosionManager.getInstance().draw(g);
        
        // 如果是调试模式，绘制调试信息
        if (debugMode) {
            drawDebugInfo(g);
        }
        
        // 绘制倒计时
        if (isCountingDown) {
            drawCountDown(g);
        }
    }

    /**
     * 绘制调试信息
     */
    private void drawDebugInfo(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        int y = 20;
        g.drawString("游戏区域: " + gameAreaWidth + "x" + gameAreaHeight, 10, y);
        y += 15;
        
        if (player != null) {
            g.drawString("玩家位置: (" + player.getX() + "," + player.getY() + ") 角度: " + 
                         Math.toDegrees(player.getAngle()), 10, y);
            y += 15;
        }
        
        if (aiTank != null) {
            g.drawString("AI位置: (" + aiTank.getX() + "," + aiTank.getY() + ") 角度: " + 
                         Math.toDegrees(aiTank.getAngle()), 10, y);
            y += 15;
            
            // 显示AI调试信息
            String aiInfo = aiTank.getAIDebugInfo();
            if (aiInfo != null && !aiInfo.isEmpty()) {
                for (String line : aiInfo.split("\n")) {
                    g.drawString(line, 10, y);
                    y += 15;
                }
            }
        }
    }

    /**
     * 处理按键事件
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (player != null && player.isAlive()) {
            player.handleKeyPress(e.getKeyCode());
        }
        
        // 切换调试模式 (F3)
        if (e.getKeyCode() == KeyEvent.VK_F3) {
            debugMode = !debugMode;
            System.out.println("调试模式: " + (debugMode ? "开启" : "关闭"));
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (player != null && player.isAlive()) {
            player.handleKeyRelease(e.getKeyCode());
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // 不需要实现
    }

    /**
     * 开始游戏
     */
    public void startGame() {
        // 如果是从暂停状态恢复，直接继续游戏
        if (isPaused) {
            gameRunning = true;
            gameTimer.start();
            isPaused = false;
            requestFocus();
            System.out.println("游戏已恢复");
            return;
        }
        
        // 确保游戏区域尺寸已更新
        gameAreaWidth = getWidth();
        gameAreaHeight = getHeight();
        
        // 如果尺寸仍无效，使用默认值
        if (gameAreaWidth <= 0) gameAreaWidth = 800;
        if (gameAreaHeight <= 0) gameAreaHeight = 600;
        
        // 更新碰撞检测器
        if (detector instanceof SimpleCollisionDetector) {
            ((SimpleCollisionDetector)detector).setGameAreaSize(
                    new Dimension(gameAreaWidth, gameAreaHeight)
            );
        }
        
        // 在游戏开始时初始化墙体
        initWalls();
        
        // 在创建墙体后随机放置玩家坦克
        randomizePlayerPosition();
        
        // 确保坦克在游戏区域内
        checkAndAdjustTankPositions();
        
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
        gameRunning = false; // 确保游戏不会在倒计时期间运行
        
        // 请求焦点，使键盘输入有效
        requestFocus();
        
        // 创建倒计时定时器，每秒触发一次
        if (countDownTimer != null) {
            countDownTimer.stop();
        }
        
        countDownTimer = new Timer(1000, e -> {
            countDownSeconds--;
            // 播放倒计时音效（如果有）
//            playCountdownSound();
            
            if (countDownSeconds <= 0) {
                // 倒计时结束，停止定时器
                ((Timer)e.getSource()).stop();
                // 真正启动游戏
                finalizeGameStart();
                isCountingDown = false;
            }
            repaint(); // 刷新显示
        });
        
        countDownTimer.setRepeats(true);
        countDownTimer.start();
        repaint(); // 立即刷新显示第一个数字
    }

    /**
     * 播放倒计时音效
     */
    private void playCountdownSound() {
        // 这里可以添加倒计时音效代码
        // 例如使用Java Sound API播放"beep"声音
    }

    /**
     * 倒计时结束后真正启动游戏
     */
    private void finalizeGameStart() {
        gameRunning = true;
        gameTimer.start();
        System.out.println("倒计时结束，游戏开始!");
    }

    /**
     * 绘制倒计时数字
     */
    private void drawCountDown(Graphics g) {
        // 保存原始颜色
        Color originalColor = g.getColor();
        
        // 设置倒计时文本属性
        int fontSize = Math.min(gameAreaWidth, gameAreaHeight) / 5; // 根据游戏区域大小调整字体
        Font countdownFont = new Font("Arial", Font.BOLD, fontSize);
        g.setFont(countdownFont);
        
        // 计算文本绘制位置（居中）
        String countdownText = String.valueOf(countDownSeconds);
        FontMetrics fm = g.getFontMetrics(countdownFont);
        int textWidth = fm.stringWidth(countdownText);
        int textHeight = fm.getHeight();
        int x = (gameAreaWidth - textWidth) / 2;
        int y = (gameAreaHeight - textHeight) / 2 + fm.getAscent();
        
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
    
    /**
     * 暂停游戏
     */
    public void stopGame() {
        if (gameRunning) {
            gameRunning = false;
            gameTimer.stop();
            isPaused = true; // 标记为暂停状态
            System.out.println("游戏已暂停");
        }
    }
    
    /**
     * 结束游戏 - 保持不变，完全结束游戏用
     */
    public void endGame() {
        gameRunning = false;
        gameTimer.stop();
        isPaused = false; // 重置暂停状态
        
        // 清除所有爆炸效果
        ExplosionManager.getInstance().clearAllExplosions();
        
        // 保存AI学习数据
        if (aiTank != null) {
            aiTank.saveLearnedData();
        }
    }
    
    /**
     * 获取碰撞检测器
     */
    public CollisionDetector getDetector() {
        return detector;
    }
    
    /**
     * 重置游戏
     */
    public void resetGame() {
        // 重置得分和关卡
        currentLevel = 1;
        playerScore = 0;
        enemyScore = 0;
        ConfigTool.setLevel("1");
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");
        
        // 重置游戏状态
        gameRunning = false;
        
        // 清除所有爆炸效果
        ExplosionManager.getInstance().clearAllExplosions();
        
        // 重置游戏组件
        walls = new ArrayList<>();
        
        // 创建玩家坦克 ,位置在屏幕外（隐藏）
        player = new PlayerTank(-100, -100, detector);
        
        // 重置AI坦克（保留学习数据）
        if (aiTank != null) {
            // 保存现有学习数据
            aiTank.saveLearnedData();
        }
        
        // 初始化游戏
        initGame();
        
        // 更新显示
        updateDisplays();
    }
    
    /**
     * 随机放置玩家坦克
     */
    private void randomizePlayerPosition() {
        if (player == null) return;
        
        Random rand = new Random();
        int margin = 100;
        int x, y;
        
        // 默认位置（如果找不到合适位置）
        x = margin;
        y = gameAreaHeight / 2;
        
        int attempts = 0;
        final int MAX_ATTEMPTS = 50;
        boolean positionFound = false;
        
        // 尝试找到不与墙体碰撞的随机位置
        while (attempts < MAX_ATTEMPTS && !positionFound) {
            // 随机生成位置，给予一定边距防止出生在边缘
            int newX = margin + rand.nextInt(Math.max(1, gameAreaWidth - 2 * margin - 64));
            int newY = margin + rand.nextInt(Math.max(1, gameAreaHeight - 2 * margin - 64));
            
            if (!isPositionBlockedByWalls(newX, newY)) {
                x = newX;
                y = newY;
                positionFound = true;
            }
            attempts++;
        }
        
        // 设置玩家坦克位置
        player.setPosition(x, y);
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
        
        // 标准化移动向量
        if (moveLength > 0) {
            moveX /= moveLength;
            moveY /= moveLength;
        }
        
        // 检查所有墙体
        for (PVEWall wall : walls) {
            if (!wall.isSolid()) {
                // 非实心墙体检查每个段落
                for (Rectangle segment : wall.getSegments()) {
                    checkSegmentCollision(result, startX, startY, endX, endY, bulletRadius, segment, moveX, moveY);
                }
            } else {
                // 实心墙体直接检查边界
                Rectangle wallBounds = wall.getCollisionBounds();
                checkSegmentCollision(result, startX, startY, endX, endY, bulletRadius, wallBounds, moveX, moveY);
            }
        }
        
        // 检查游戏边界
        checkBoundaryCollision(result, startX, startY, endX, endY, bulletRadius);
        
        return result;
    }

    /**
     * 检查与墙体段落的碰撞
     */
    private void checkSegmentCollision(
            BulletCollisionResult result, double startX, double startY, 
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
            startX, startY, endX, endY, expandedSegment);
        
        if (intersection[0] >= 0 && intersection[0] <= 1) {
            // 找到碰撞点
            double hitX = startX + intersection[0] * (endX - startX);
            double hitY = startY + intersection[0] * (endY - startY);
            
            // 计算距离
            double distance = intersection[0] * Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
            
            // 如果这是最近的碰撞点，更新结果
            if (distance < result.distance) {
                result.hasCollided = true;
                result.distance = distance;
                result.collisionX = hitX;
                result.collisionY = hitY;
                result.collidedRect = segment;
                
                // 计算碰撞法线
                determineCollisionNormal(result, hitX, hitY, segment, moveX, moveY);
                
                // 计算穿透深度 - 防止卡墙
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
     * 处理子弹反弹
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
        double safetyMultiplier = 2.0; // 增加安全距离系数
        double offsetX = collision.normalX * (collision.penetrationDepth * safetyMultiplier);
        double offsetY = collision.normalY * (collision.penetrationDepth * safetyMultiplier);
        
        // 如果是超薄墙体，增加额外偏移以防止穿墙
        if (collision.collidedRect != null) {
            if (collision.collidedRect.width < 10 || collision.collidedRect.height < 10) {
                offsetX *= 2;
                offsetY *= 2;
            }
        }
        
        // 设置子弹位置，确保不会卡在墙内
        bullet.setPosition((int)(collision.collisionX + offsetX), (int)(collision.collisionY + offsetY));
        
        // 减少速度模拟能量损失
        bullet.decreaseSpeed(0.9);
        
        // 增加反弹计数
        bullet.bounce();
    }

    /**
     * 处理敌方子弹的反弹
     */
    private void processEnemyBulletBounce(EnemyBullet bullet, BulletCollisionResult collision) {
        // 获取子弹当前角度
        double angle = bullet.getAngle();
        double speed = bullet.getSpeed();
        
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
        double safetyMultiplier = 1.5; // 额外安全距离系数
        double offsetX = collision.normalX * (collision.penetrationDepth * safetyMultiplier);
        double offsetY = collision.normalY * (collision.penetrationDepth * safetyMultiplier);
        
        // 如果是超薄墙体，增加额外偏移以防止穿墙
        if (collision.collidedRect != null) {
            if (collision.collidedRect.width < 10 || collision.collidedRect.height < 10) {
                offsetX *= 2;
                offsetY *= 2;
            }
        }
        
        // 设置子弹位置，确保不会卡在墙内
        bullet.setPosition((int)(collision.collisionX + offsetX), (int)(collision.collisionY + offsetY));
        
        // 减少速度模拟能量损失（每次反弹损失10%能量）
        bullet.decreaseSpeed(0.9);
        
        // 增加反弹计数
        bullet.bounce();

    }
    
    /**
     * 处理AI子弹反弹
     */
    private void processAIBulletBounce(EnemyBullet bullet, BulletCollisionResult collision) {
        // 获取当前角度
        double angle = bullet.getAngle();
        
        // 计算入射向量
        double incidentX = Math.cos(angle);
        double incidentY = Math.sin(angle);
        
        // 计算反射向量 R = I - 2(I·N)N
        double dot = incidentX * collision.normalX + incidentY * collision.normalY;
        double reflectX = incidentX - 2 * dot * collision.normalX;
        double reflectY = incidentY - 2 * dot * collision.normalY;
        
        // 计算新角度
        double newAngle = Math.atan2(reflectY, reflectX);
        
        // 设置子弹新角度
        bullet.setAngle(newAngle);
        
        // 特殊位置调整 - 防止子弹卡墙
        double safetyFactor;
        
        // 根据碰撞类型调整安全系数
        if (collision.isBoundaryCollision) {
            safetyFactor = 3.0; // 边界碰撞使用更大系数
        } else if (collision.isCornerCollision) {
            safetyFactor = 2.5; // 角落碰撞使用中等系数
        } else {
            safetyFactor = 2.0; // 普通墙体碰撞
        }
        
        // 计算安全偏移量
        double offsetX = collision.normalX * (bullet.getRadius() * safetyFactor);
        double offsetY = collision.normalY * (bullet.getRadius() * safetyFactor);
        
        // 设置子弹新位置，确保不会卡墙
        bullet.setPosition(
            (int)(collision.collisionX + offsetX), 
            (int)(collision.collisionY + offsetY)
        );
        
        // 减少速度模拟能量损失
        bullet.decreaseSpeed(0.92);
        
        // 添加微小随机扰动防止循环反弹
        if (Math.random() < 0.15) {
            double noise = (Math.random() - 0.5) * 0.08;
            bullet.setAngle(newAngle + noise);
        }
        
        // 增加反弹计数
        bullet.bounce();
    }
    
    /**
     * 计算子弹轨迹包围盒，用于快速排除不可能碰撞的墙体
     */
    private Rectangle expandBulletPath(Rectangle bulletBounds, double dx, double dy, double speed) {
        // 计算子弹完整路径的包围盒
        int x = bulletBounds.x;
        int y = bulletBounds.y;
        int width = bulletBounds.width;
        int height = bulletBounds.height;
        
        // 如果是向右移动
        if (dx > 0) {
            width += (int)(dx * speed);
        } 
        // 如果是向左移动
        else if (dx < 0) {
            x += (int)(dx * speed);
            width -= (int)(dx * speed);
        }
        
        // 如果是向下移动
        if (dy > 0) {
            height += (int)(dy * speed);
        } 
        // 如果是向上移动
        else if (dy < 0) {
            y += (int)(dy * speed);
            height -= (int)(dy * speed);
        }
        
        return new Rectangle(x, y, width, height);
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
        Rectangle wallRect = null;         // 碰撞的墙体矩形
        boolean isBoundaryCollision = false; // 是否是边界碰撞
        boolean isCornerCollision = false;   // 是否是角落碰撞
        double distance = Double.MAX_VALUE;  // 碰撞距离
        Rectangle collidedRect = null;       // 碰撞的矩形
    }

    /**
     * 计算穿透深度
     */
    private double calculatePenetrationDepth(double hitX, double hitY, Rectangle wall, double normalX, double normalY) {
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
     * 计算射线与矩形的交点
     * @param startX 射线起点X
     * @param startY 射线起点Y
     * @param endX 射线终点X
     * @param endY 射线终点Y
     * @param rect 目标矩形
     * @return 长度为2的数组，第一个元素是参数t (0<=t<=1表示交点在线段上)，第二个是碰撞面索引
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
                sides[0] = 0; // 左边
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
                sides[1] = 1; // 右边
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
                sides[2] = 2; // 上边
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
                sides[3] = 3; // 下边
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
    
    private void calculateAIPenetrationDepth(
            BulletCollisionResult result, double bulletX, double bulletY, Rectangle wall) {
    
        // 计算子弹中心到墙体最近边的距离
        double distX, distY;
        
        // X方向距离
        if (bulletX < wall.x) {
            distX = wall.x - bulletX;
        } else if (bulletX > wall.x + wall.width) {
            distX = bulletX - (wall.x + wall.width);
        } else {
            distX = 0; // 子弹在墙体X范围内
        }
        
        // Y方向距离
        if (bulletY < wall.y) {
            distY = wall.y - bulletY;
        } else if (bulletY > wall.y + wall.height) {
            distY = bulletY - (wall.y + wall.height);
        } else {
            distY = 0; // 子弹在墙体Y范围内
        }
        
        // 设置穿透深度
        if (distX == 0 && distY == 0) {
            // 子弹完全在墙内，使用较大的穿透深度
            result.penetrationDepth = Math.max(wall.width, wall.height) * 0.5;
        } else if (distX == 0) {
            // 子弹在墙体X范围内，Y方向穿透
            result.penetrationDepth = distY;
        } else if (distY == 0) {
            // 子弹在墙体Y范围内，X方向穿透
            result.penetrationDepth = distX;
        } else {
            // 子弹在墙角，计算对角线距离
            result.penetrationDepth = Math.sqrt(distX * distX + distY * distY);
        }
    }
    
    /**
     * 检查与游戏边界的碰撞
     */
    private void checkBoundaryCollision(
            BulletCollisionResult result, double startX, double startY, 
            double endX, double endY, double bulletRadius) {
    
        int gameWidth = getWidth();
        int gameHeight = getHeight();
        
        // 左边界
        if (endX - bulletRadius < 0) {
            double t = (bulletRadius - startX) / (endX - startX);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = 0 + bulletRadius;
                result.collisionY = startY + t * (endY - startY);
                result.normalX = 1;
                result.normalY = 0;
                result.penetrationDepth = bulletRadius;
            }
        }
        
        // 右边界
        if (endX + bulletRadius > gameWidth) {
            double t = (gameWidth - bulletRadius - startX) / (endX - startX);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = gameWidth - bulletRadius;
                result.collisionY = startY + t * (endY - startY);
                result.normalX = -1;
                result.normalY = 0;
                result.penetrationDepth = bulletRadius;
            }
        }
        
        // 上边界
        if (endY - bulletRadius < 0) {
            double t = (bulletRadius - startY) / (endY - startY);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = startX + t * (endX - startX);
                result.collisionY = 0 + bulletRadius;
                result.normalX = 0;
                result.normalY = 1;
                result.penetrationDepth = bulletRadius;
            }
        }
        
        // 下边界
        if (endY + bulletRadius > gameHeight) {
            double t = (gameHeight - bulletRadius - startY) / (endY - startY);
            if (t >= 0 && t <= 1 && t < result.distance) {
                result.hasCollided = true;
                result.distance = t;
                result.collisionX = startX + t * (endX - startX);
                result.collisionY = gameHeight - bulletRadius;
                result.normalX = 0;
                result.normalY = -1;
                result.penetrationDepth = bulletRadius;
            }
        }
    }

    /**
     * 优化的AI子弹碰撞检测方法
     */
    private BulletCollisionResult detectAIBulletCollision(
            double startX, double startY, double endX, double endY, double bulletRadius) {
        
        BulletCollisionResult result = new BulletCollisionResult();
        
        // 计算移动距离决定采样点数量
        double moveLength = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        
        // 动态调整采样点
        int samplePoints = 3; // 基础采样点
        if (moveLength > 15) samplePoints = 5;
        if (moveLength > 25) samplePoints = 7;
        
        // 计算移动单位向量
        double moveX = endX - startX;
        double moveY = endY - startY;
        if (moveLength > 0) {
            moveX /= moveLength;
            moveY /= moveLength;
        }
        
        // 使用多点采样检测碰撞
        for (int i = 0; i <= samplePoints; i++) {
            double t = (double) i / samplePoints;
            double sampleX = startX + t * (endX - startX);
            double sampleY = startY + t * (endY - startY);
            
            // 检查墙体碰撞
            for (PVEWall wall : walls) {
                if (wall.isSolid()) {
                    // 实心墙直接检查
                    checkAIBulletWallCollision(result, sampleX, sampleY, wall.getCollisionBounds(), 
                                              bulletRadius, moveX, moveY);
                } else {
                    // 非实心墙检查每个段落
                    for (Rectangle segment : wall.getSegments()) {
                        checkAIBulletWallCollision(result, sampleX, sampleY, segment, 
                                                  bulletRadius, moveX, moveY);
                    }
                }
                
                // 如果已检测到碰撞，停止检查
                if (result.hasCollided) break;
            }
            
            // 如果已检测到碰撞，停止检查后续点
            if (result.hasCollided) break;
        }
        
        // 检查边界碰撞
        if (!result.hasCollided) {
            checkAIBulletBoundaryCollision(result, startX, startY, endX, endY, bulletRadius);
        }
        
        return result;
    }

    /**
     * 检查AI子弹与墙体的碰撞
     */
    private void checkAIBulletWallCollision(
            BulletCollisionResult result, double bulletX, double bulletY, 
            Rectangle wall, double bulletRadius, double moveX, double moveY) {
    
        // 扩展墙体边界，考虑子弹半径
        Rectangle expandedWall = new Rectangle(
            wall.x - (int)bulletRadius,
            wall.y - (int)bulletRadius,
            wall.width + (int)(bulletRadius * 2),
            wall.height + (int)(bulletRadius * 2)
        );
        
        // 检查子弹是否在扩展墙体内
        if (expandedWall.contains(bulletX, bulletY)) {
            result.hasCollided = true;
            result.collisionX = bulletX;
            result.collisionY = bulletY;
            result.wallRect = wall;
            
            // 计算碰撞法线
            determineAIBulletCollisionNormal(result, bulletX, bulletY, wall, moveX, moveY);
            
            // 计算穿透深度
            calculateAIPenetrationDepth(result, bulletX, bulletY, wall);
        }
    }

    /**
     * 确定AI子弹碰撞法线
     */
    private void determineAIBulletCollisionNormal(
            BulletCollisionResult result, double hitX, double hitY, 
            Rectangle wall, double moveX, double moveY) {
        
        // 计算子弹到墙体各边的距离
        double distLeft = Math.abs(hitX - wall.x);
        double distRight = Math.abs(hitX - (wall.x + wall.width));
        double distTop = Math.abs(hitY - wall.y);
        double distBottom = Math.abs(hitY - (wall.y + wall.height));
        
        // 找出最近的边
        double minDist = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));
        
        // 设置法线方向
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
        
        // 处理墙角碰撞 - 更精确的法线计算
        double cornerThreshold = 5.0;
        boolean nearCorner = false;
        
        // 检查是否靠近四个角落
        if (distLeft < cornerThreshold && distTop < cornerThreshold) {
            // 左上角
            double dx = hitX - wall.x;
            double dy = hitY - wall.y;
            double len = Math.sqrt(dx*dx + dy*dy);
            result.normalX = dx / len;
            result.normalY = dy / len;
            nearCorner = true;
        } else if (distRight < cornerThreshold && distTop < cornerThreshold) {
            // 右上角
            double dx = hitX - (wall.x + wall.width);
            double dy = hitY - wall.y;
            double len = Math.sqrt(dx*dx + dy*dy);
            result.normalX = dx / len;
            result.normalY = dy / len;
            nearCorner = true;
        } else if (distLeft < cornerThreshold && distBottom < cornerThreshold) {
            // 左下角
            double dx = hitX - wall.x;
            double dy = hitY - (wall.y + wall.height);
            double len = Math.sqrt(dx*dx + dy*dy);
            result.normalX = dx / len;
            result.normalY = dy / len;
            nearCorner = true;
        } else if (distRight < cornerThreshold && distBottom < cornerThreshold) {
            // 右下角
            double dx = hitX - (wall.x + wall.width);
            double dy = hitY - (wall.y + wall.height);
            double len = Math.sqrt(dx*dx + dy*dy);
            result.normalX = dx / len;
            result.normalY = dy / len;
            nearCorner = true;
        }
        
        result.isCornerCollision = nearCorner;
    }
    
    /**
     * 检查AI子弹与游戏边界的碰撞
     */
    private void checkAIBulletBoundaryCollision(
            BulletCollisionResult result, double startX, double startY, 
            double endX, double endY, double bulletRadius) {
    
        int gameWidth = getWidth();
        int gameHeight = getHeight();
        
        // 增加边界安全偏移量
        double safetyOffset = 2.0;
        double adjustedRadius = bulletRadius + safetyOffset;
        
        // 左边界
        if (endX - adjustedRadius < 0) {
            result.hasCollided = true;
            result.collisionX = adjustedRadius;
            result.collisionY = endY;
            result.normalX = 1;
            result.normalY = 0;
            result.penetrationDepth = adjustedRadius;
            result.isBoundaryCollision = true;
        }
        // 右边界
        else if (endX + adjustedRadius > gameWidth) {
            result.hasCollided = true;
            result.collisionX = gameWidth - adjustedRadius;
            result.collisionY = endY;
            result.normalX = -1;
            result.normalY = 0;
            result.penetrationDepth = adjustedRadius;
            result.isBoundaryCollision = true;
        }
        // 上边界
        else if (endY - adjustedRadius < 0) {
            result.hasCollided = true;
            result.collisionX = endX;
            result.collisionY = adjustedRadius;
            result.normalX = 0;
            result.normalY = 1;
            result.penetrationDepth = adjustedRadius;
            result.isBoundaryCollision = true;
        }
        // 下边界
        else if (endY + adjustedRadius > gameHeight) {
            result.hasCollided = true;
            result.collisionX = endX;
            result.collisionY = gameHeight - adjustedRadius;
            result.normalX = 0;
            result.normalY = -1;
            result.penetrationDepth = adjustedRadius;
            result.isBoundaryCollision = true;
        }
    }
}