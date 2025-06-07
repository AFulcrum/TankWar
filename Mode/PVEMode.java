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
    private int gameAreaWidth;
    private int gameAreaHeight;
    private PlayerTank player;
    private AITank aiTank;
    private ArrayList<PVEWall> walls;
    private Timer gameTimer;
    private boolean gameRunning;
    private CollisionDetector detector;
    private JLabel levelLabel;
    private JLabel scoreLabel;
    private int currentLevel;
    private int playerScore;
    private int enemyScore;
    private static final int SCORE_TO_WIN = 3;
    private static final int SCORE_TO_LOSE = 10; // 玩家被击中10次后游戏结束
    private static final int RESPAWN_DISTANCE = 150; // 重生时最小距离

    public PVEMode(CollisionDetector detector, JLabel levelLabel, JLabel scoreLabel) {
        this.detector = detector;
        this.levelLabel = levelLabel;
        this.scoreLabel = scoreLabel;
        this.currentLevel = ConfigTool.getLevel();
        this.playerScore = ConfigTool.getOurScore();
        this.enemyScore = ConfigTool.getEnemyScore();

        setLayout(null);
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 更新游戏区域尺寸
                gameAreaWidth = getWidth();
                gameAreaHeight = getHeight();
                
                System.out.println("窗口大小变化: " + gameAreaWidth + "x" + gameAreaHeight);

                // 更新碰撞检测器的游戏区域大小
                if (detector instanceof SimpleCollisionDetector) {
                    ((SimpleCollisionDetector)detector).setGameAreaSize(
                            new Dimension(gameAreaWidth, gameAreaHeight)
                    );
                }

                // 确保坦克在新边界内
                checkAndAdjustTankPositions();
                
                // 当窗口大小变化时重新初始化墙体
                if (gameRunning && walls != null) {
                    // 强制重新生成适合新尺寸的墙体
                    initWalls();
                }
            }
        });

        initGame();

        gameTimer = new Timer(16, e -> {
            if (gameRunning) {
                updateGame();
                repaint();
            }
        });
    }

    private void initGame() {
        walls = new ArrayList<>();
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
        
        // 在这里先不初始化墙体，等到实际开始游戏时再初始化
        // 只创建玩家和AI坦克
        
        // 只在第一次初始化AI坦克
        if (aiTank == null) {
            spawnAITank();
        } else {
            // 已存在则只重置状态和位置
            resetAITank();
        }
        updateDisplays();
    }
    private void resetAITank() {
        if (aiTank != null) {
            // 生成新的随机位置
            Random rand = new Random();
            int margin = 100;
            int x, y;
            int attempts = 0;
            final int MAX_ATTEMPTS = 50;

            do {
                x = margin + rand.nextInt(Math.max(1, gameAreaWidth - 2 * margin));
                y = margin + rand.nextInt(Math.max(1, gameAreaHeight - 2 * margin));
                attempts++;

                if (attempts >= MAX_ATTEMPTS) {
                    x = margin;
                    y = margin;
                    break;
                }
            } while (distance(x, y, player.getX(), player.getY()) < RESPAWN_DISTANCE ||
                    isPositionBlocked(x, y));

            // 只更新位置和状态，保持AI学习数据
            aiTank.setPosition(x, y);
            aiTank.revive();
        }
    }

    private void initWalls() {
        walls.clear(); // 清除已有墙体

        // 获取游戏区域大小 - 确保使用正确的尺寸
        int areaWidth = getWidth();
        int areaHeight = getHeight();
        
        // 如果尺寸无效，使用默认值，但确保这些值足够大
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

            // 创建墙体碰撞矩形列表
            List<Rectangle> wallBounds = new ArrayList<>();
            for (PVEWall wall : walls) {
                if (wall.getSegments().size() > 1) {
                    // 对于复杂形状的墙体，添加每个段落
                    wallBounds.addAll(wall.getSegments());
                } else {
                    // 对于简单墙体，添加整个边界
                    wallBounds.add(wall.getCollisionBounds());
                }
            }

            // 更新检测器中的墙体信息
//            simpleDetector.setWalls(wallBounds);
        }
    }

    private void checkAndAdjustTankPositions() {
        // 调整玩家坦克位置
        if (player != null) {
            int x = Math.min(Math.max(player.getX(), 0), gameAreaWidth - player.getWidth());
            int y = Math.min(Math.max(player.getY(), 0), gameAreaHeight - player.getHeight());
            player.setPosition(x, y);
        }

        // 调整AI坦克位置
        if (aiTank != null) {
            int x = Math.min(Math.max(aiTank.getX(), 0), gameAreaWidth - aiTank.getWidth());
            int y = Math.min(Math.max(aiTank.getY(), 0), gameAreaHeight - aiTank.getHeight());
            aiTank.setPosition(x, y);
        }
    }

    private void spawnAITank() {
        Random rand = new Random();
        int margin = 100;
        int x, y;
        int attempts = 0;
        final int MAX_ATTEMPTS = 50;

        do {
            x = margin + rand.nextInt(Math.max(1, gameAreaWidth - 2 * margin));
            y = margin + rand.nextInt(Math.max(1, gameAreaHeight - 2 * margin));
            attempts++;

            if (attempts >= MAX_ATTEMPTS) {
                x = margin;
                y = margin;
                break;
            }
        } while (distance(x, y, player.getX(), player.getY()) < RESPAWN_DISTANCE ||
                isPositionBlocked(x, y));

        if (aiTank == null) {
            aiTank = new AITank(x, y, detector);
        } else {
            aiTank.setPosition(x, y);
            aiTank.revive();
        }
    }
    // 检查位置是否被阻挡
    private boolean isPositionBlocked(int x, int y) {
        // 检查边界
        if (x < 0 || y < 0 || x + 64 > gameAreaWidth || y + 64 > gameAreaHeight) {
            return true;
        }

        Rectangle newPos = new Rectangle(x, y, 64, 64);

        // 检查与墙体的碰撞
        for (PVEWall wall : walls) {
            if (newPos.intersects(wall.getCollisionBounds())) {
                return true;
            }
        }

        // 检查与玩家的距离
        if (player != null && player.isAlive()) {
            if (distance(x, y, player.getX(), player.getY()) < RESPAWN_DISTANCE) {
                return true;
            }
        }

        return false;
    }

    private void respawnTank(AbstractTank tank, AbstractTank other) {
        Random rand = new Random();
        int areaWidth = getWidth() <= 0 ? 800 : getWidth();
        int areaHeight = getHeight() <= 0 ? 600 : getHeight();
        int margin = 50;
        int x, y;
        int attempts = 0;
        final int MAX_ATTEMPTS = 50;

        do {
            x = margin + rand.nextInt(areaWidth - 2 * margin);
            y = margin + rand.nextInt(areaHeight - 2 * margin);
            attempts++;

            if (attempts >= MAX_ATTEMPTS) {
                x = margin;
                y = margin;
                break;
            }
        } while (distance(x, y, other.getX(), other.getY()) < RESPAWN_DISTANCE ||
                isPositionBlocked(x, y));

        tank.setPosition(x, y);
        tank.revive();
        
        // 如果是玩家坦克，重置其键盘状态
        if (tank instanceof PlayerTank) {
            ((PlayerTank)tank).resetKeyStates();
        }
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
    }

    private void updateGame() {
        if (player.isAlive()) {
            player.updateMovement();
            player.updateBullets();
        }
        if (aiTank != null && aiTank.isAlive()) {
            aiTank.updateAI(player, currentLevel);
        }
        checkCollisions();
        checkBulletWallCollisions();
        checkScores();
        updateDisplays();
        player.updateBullets();
        if (aiTank != null) {
            aiTank.updateBullets();
        }

        // 添加墙体碰撞检测
        checkBulletWallCollisions();

        // 更新爆炸效果
        ExplosionManager.getInstance().update();
    }
    // 新增墙体碰撞检测方法
    private void checkBulletWallCollisions() {
        // 检测玩家子弹与墙体碰撞
        for (PlayerBullet bullet : player.getBullets()) {
            if (!bullet.isActive()) continue;
            for (PVEWall wall : walls) {
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

    private void checkCollisions() {
        // 玩家子弹击中AI
        if (player.isAlive() && aiTank != null && aiTank.isAlive()) {
            for (PlayerBullet bullet : player.getBullets()) {
                if (!bullet.isActive()) continue;
                if (bullet.getCollisionBounds().intersects(aiTank.getCollisionBounds())) {
                    bullet.deactivate();
                    aiTank.setAlive(false);
                    // 在AI坦克死亡时调用学习方法
                    aiTank.onDeath(player);
                    playerScore++;
                    ConfigTool.setOurScore(String.valueOf(playerScore));
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
                if (bullet.getCollisionBounds().intersects(player.getCollisionBounds())) {
                    bullet.deactivate();
                    player.setAlive(false);
                    
                    // 重置玩家坦克的键盘状态
                    player.resetKeyStates();
                    
                    enemyScore++;
                    ConfigTool.setEnemyScore(String.valueOf(enemyScore));
                    
                    // 检查是否达到游戏结束条件（玩家被击中10次）
                    if (enemyScore >= SCORE_TO_LOSE) { // SCORE_TO_LOSE设为10
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

    private void checkScores() {
        if (playerScore >= SCORE_TO_WIN) {
            advanceToNextLevel();
        } else if (enemyScore >= SCORE_TO_LOSE) {
            gameOver();
        }
    }

    private void gameOver() {
        // 确保只执行一次
        if (!gameRunning) return;
        
        gameRunning = false;
        gameTimer.stop();
        
        // 先保存AI学习数据和重置游戏数据
        if (aiTank != null) {
            aiTank.saveLearnedData();
        }
        
        // 重置游戏数据
        ConfigTool.setLevel("1");
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");
        
        // 使用直接调用而不是invokeLater，避免事件队列问题
        JOptionPane.showMessageDialog(this, 
            "游戏结束！\n止步于第 " + currentLevel + " 关\n我方得分: " + playerScore + "\n敌方得分: " + enemyScore,
            "游戏结束", JOptionPane.INFORMATION_MESSAGE);

        // 使用另一个invokeLater来确保UI线程处理返回主菜单
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

    private void advanceToNextLevel() {
        currentLevel++;
        ConfigTool.setLevel(String.valueOf(currentLevel));
        playerScore = 0;
        enemyScore = 0;
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");

        // 只重置地图和坦克位置，不重新生成AI坦克
        walls = new ArrayList<>();
        player = new PlayerTank(50, 50, detector);
        initWalls();

        // 只重置AI坦克位置，保持AI学习数据
        if (aiTank != null) {
            resetAITank();  // 第一次重置位置
        }

        updateDisplays();
        gameRunning = true;
    }

    private void updateDisplays() {
        levelLabel.setText("<html><div style='text-align: center;'>第<br>"
                + currentLevel + "<br>关</div></html>");
        scoreLabel.setText("<html><div style='text-align: center;'>我方<br>"
                + playerScore + ":" + enemyScore + "<br>敌方</div></html>");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 只在游戏运行时绘制墙体
        if (gameRunning && walls != null) {
            for (PVEWall wall : walls) {
                wall.draw(g);
            }
        }
        
        // 绘制坦克
        if (player != null) {
            player.draw(g);
        }
        
        if (aiTank != null) {
            aiTank.draw(g);
        }
        
        // 最后绘制爆炸效果（最高优先级）
        ExplosionManager.getInstance().draw(g);
    }

    private void drawTank(Graphics2D g2d, AbstractTank tank) {
        int x = tank.getX();
        int y = tank.getY();
        int width = tank.getWidth();
        int height = tank.getHeight();
        double angle = tank.getAngle();
        g2d.translate(x + width / 2, y + height / 2);
        g2d.rotate(angle);
        g2d.drawImage(tank.getCurrentImage(), -width / 2, -height / 2, width, height, null);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (player != null && player.isAlive()) {
            player.handleKeyPress(e.getKeyCode());
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if (player != null && player.isAlive()) {
            player.handleKeyRelease(e.getKeyCode());
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {}

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
        
        // 重新定位坦克，确保它们在有效位置
        repositionTanks();
        
        // 只在游戏未运行时才启动
        if (!gameRunning) {
            gameRunning = true;
            gameTimer.start();
            requestFocus();
        }
    }
    public void stopGame() {
        gameRunning = false;
        gameTimer.stop();
    }
    public void endGame() {
        gameRunning = false;
        gameTimer.stop();
        
        // 保存AI学习数据
        if (aiTank != null) {
            aiTank.saveLearnedData();
        }
    }
    public CollisionDetector getDetector() {
        return detector;
    }
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

    // 添加一个重新定位坦克的方法
    private void repositionTanks() {
        if (player != null) {
            // 将玩家坦克放在游戏区域左侧中间位置
            int playerX = Math.min(50, gameAreaWidth / 10);
            int playerY = gameAreaHeight / 2 - player.getHeight() / 2;
            player.setPosition(playerX, playerY);
        }
        
        if (aiTank != null) {
            // 将AI坦克放在游戏区域右侧中间位置
            int aiX = Math.max(gameAreaWidth - 50 - aiTank.getWidth(), gameAreaWidth * 9 / 10 - aiTank.getWidth());
            int aiY = gameAreaHeight / 2 - aiTank.getHeight() / 2;
            aiTank.setPosition(aiX, aiY);
        }
    }
}