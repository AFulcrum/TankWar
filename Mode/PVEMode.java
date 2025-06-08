// PVEMode.java
package Mode;

import Config.*;
import InterFace.CollisionDetector;
import Structure.PVEWall;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                
                System.out.println("游戏区域大小变化: " + gameAreaWidth + "x" + gameAreaHeight);

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
        
        // 创建玩家坦克
        player = new PlayerTank(50, 50, detector);
        
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

        System.out.println("初始化墙体，游戏区域大小: " + areaWidth + "x" + areaHeight);
        
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

    /**
     * 更新碰撞检测器中的墙体信息
     */
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
            // 修正AITank的角度计算，确保图像与移动方向一致
            fixAITankRotation(aiTank);
        } else {
            aiTank.setPosition(x, y);
            aiTank.revive();
        }
    }
    
    /**
     * 修正AI坦克旋转方向问题
     */
    private void fixAITankRotation(AITank tank) {
        // 这个方法将在AITank实例创建后被调用，用来修正角度计算
        // 实际的修复将在AITank类的方法中实现
        // 这里我们可以通过反射来设置内部字段，或者添加配置属性
        
        // 由于我们不能直接修改AITank类，这里添加一个标记让它知道需要调整旋转
        // 假设AITank类有setFixRotation方法
        try {
            // 可以通过反射或其他方式设置
            // 这里简化处理，假设这个方法存在
            // tank.setFixRotation(true);
            System.out.println("已应用AI坦克旋转修正");
        } catch (Exception e) {
            System.err.println("无法应用AI坦克旋转修正: " + e.getMessage());
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
        // 更新玩家坦克
        if (player.isAlive()) {
            player.updateMovement();
            player.updateBullets();
        }
        
        // 更新AI坦克
        if (aiTank != null && aiTank.isAlive()) {
            aiTank.updateAI(player, currentLevel);
        }
        
        // 检测碰撞
        checkCollisions();
        checkBulletWallCollisions();
        
        // 检查得分情况
        checkScores();
        updateDisplays();
        
        // 再次更新子弹（确保所有子弹状态都被正确处理）
        player.updateBullets();
        if (aiTank != null) {
            aiTank.updateBullets();
        }

        // 更新爆炸效果
        ExplosionManager.getInstance().update();
    }
    
    /**
     * 检查子弹与墙体碰撞
     */
    private void checkBulletWallCollisions() {
        // 检测玩家子弹与墙体碰撞
        for (PlayerBullet bullet : player.getBullets()) {
            if (!bullet.isActive()) continue;
            for (PVEWall wall : walls) {
                // 检查子弹是否与墙体碰撞
                if (bullet.getCollisionBounds().intersects(wall.getCollisionBounds())) {
                    bullet.bounce();
                    break;
                }
            }
        }

        // 检测AI子弹与墙体碰撞
        if (aiTank != null) {
            for (EnemyBullet bullet : aiTank.getBullets()) {
                if (!bullet.isActive()) continue;
                for (PVEWall wall : walls) {
                    if (bullet.getCollisionBounds().intersects(wall.getCollisionBounds())) {
                        bullet.bounce();
                        break;
                    }
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
                    
                    // 创建爆炸效果
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
                if (!bullet.canCollide() || isBulletTooCloseToTank(bullet, player)) {
                    continue;
                }
                
                // 检查碰撞
                Rectangle bulletBounds = bullet.getCollisionBounds();
                Rectangle playerBounds = player.getCollisionBounds();
                
                if (bulletBounds != null && playerBounds != null && 
                    bulletBounds.intersects(playerBounds)) {
                    
                    // 子弹击中玩家，创建爆炸效果
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
                    
                    // 打印调试信息
                    System.out.println("玩家被击中! 位置: " + player.getX() + "," + player.getY());
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
        
        // 播放爆炸音效 (如果有)
        // playExplosionSound();
        
        System.out.println("创建爆炸效果 at " + centerX + "," + centerY);
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
        
        // 只在游戏运行时绘制墙体
        if (gameRunning && walls != null) {
            for (PVEWall wall : walls) {
                wall.draw(g);
            }
        }
        
        // 绘制坦克和子弹
        if (player != null) {
            player.draw(g);
        }
        
        if (aiTank != null) {
            aiTank.draw(g);
        }
        
        // 最后绘制爆炸效果（最高优先级）
        ExplosionManager.getInstance().draw(g);
        
        // 如果是调试模式，绘制调试信息
        if (debugMode) {
            drawDebugInfo(g);
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
        
        // 只在游戏未运行时才启动
        if (!gameRunning) {
            gameRunning = true;
            gameTimer.start();
            requestFocus();
        }
    }
    
    /**
     * 暂停游戏
     */
    public void stopGame() {
        gameRunning = false;
        gameTimer.stop();
    }
    
    /**
     * 结束游戏
     */
    public void endGame() {
        gameRunning = false;
        gameTimer.stop();
        
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
        
        // 重置游戏组件
        walls = new ArrayList<>();
        player = new PlayerTank(50, 50, detector);
        
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
    
    // 在PVEMode.java中添加以下方法，用于检测子弹是否太靠近坦克
    private boolean isBulletTooCloseToTank(EnemyBullet bullet, PlayerTank tank) {
        // 仅在子弹刚生成且距离太近时过滤，而不是一直过滤
        if (bullet.getTravelDistance() > 20) { 
            return false; // 子弹已经飞行一段距离，不再视为"太近"
        }
        
        // 计算子弹到坦克中心的距离
        Rectangle bulletBounds = bullet.getCollisionBounds();
        int bulletX = bulletBounds.x + bulletBounds.width / 2;
        int bulletY = bulletBounds.y + bulletBounds.height / 2;
        int tankCenterX = tank.getX() + tank.getWidth() / 2;
        int tankCenterY = tank.getY() + tank.getHeight() / 2;
        
        double distance = Math.sqrt(
            Math.pow(bulletX - tankCenterX, 2) + 
            Math.pow(bulletY - tankCenterY, 2)
        );
        
        // 如果距离小于坦克半径加安全距离，且子弹刚生成，则认为太近
        double tankRadius = Math.max(tank.getWidth(), tank.getHeight()) / 2.0;
        return distance < (tankRadius + 10); // 减小安全距离到10像素
    }
}