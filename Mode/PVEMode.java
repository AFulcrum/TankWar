// PVEMode.java
package Mode;

import Config.*;
import InterFace.CollisionDetector;
import Structure.PVEWall;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
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
    private static final int SCORE_TO_LOSE = 3;
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
                gameAreaWidth = getWidth();
                gameAreaHeight = getHeight();

                // 更新碰撞检测器的游戏区域大小
                if (detector instanceof SimpleCollisionDetector) {
                    ((SimpleCollisionDetector)detector).setGameAreaSize(
                            new Dimension(gameAreaWidth, gameAreaHeight)
                    );
                }

                // 确保坦克在新边界内
                checkAndAdjustTankPositions();

                // 重新布局时重置AI坦克
                if (aiTank == null) {
                    spawnAITank();
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
        aiTank = null;
        initWalls();
        spawnAITank();
        updateDisplays();
    }

    private void initWalls() {
        Random random = new Random();
        int wallCount = 5 + currentLevel * 2;
        int areaWidth = getWidth() <= 0 ? 800 : getWidth();
        int areaHeight = getHeight() <= 0 ? 600 : getHeight();

        for (int i = 0; i < wallCount; i++) {
            int x = random.nextInt(Math.max(1, areaWidth - 40));
            int y = random.nextInt(Math.max(1, areaHeight - 40));
            walls.add(new PVEWall(x, y));
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
                    enemyScore++;
                    ConfigTool.setEnemyScore(String.valueOf(enemyScore));
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
        gameRunning = false;
        gameTimer.stop();
        JOptionPane.showMessageDialog(this, "游戏结束！\n我方得分: " + playerScore + "\n敌方得分: " + enemyScore,
                "游戏结束", JOptionPane.INFORMATION_MESSAGE);
        ConfigTool.setLevel("1");
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");
        currentLevel = 1;
        playerScore = 0;
        enemyScore = 0;
        initGame();
    }

    private void advanceToNextLevel() {
        currentLevel++;
        ConfigTool.setLevel(String.valueOf(currentLevel));
        playerScore = 0;
        enemyScore = 0;
        ConfigTool.setOurScore("0");
        ConfigTool.setEnemyScore("0");
        initGame();
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
        for (PVEWall wall : walls) {
            wall.draw(g);
        }
        // 绘制玩家
        if (player != null && player.isAlive() && player.getCurrentImage() != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            drawTank(g2d, player);
            player.drawBullets(g);
            g2d.dispose();
        }
        // 绘制AI
        if (aiTank != null && aiTank.isAlive() && aiTank.getCurrentImage() != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            drawTank(g2d, aiTank);
            aiTank.drawBullets(g);
            g2d.dispose();
        }
        player.drawBullets(g);
        if (aiTank != null) {
            aiTank.drawBullets(g);
        }
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
        gameRunning = true;
        gameTimer.start();
        requestFocus();
    }
    public void stopGame() {
        gameRunning = false;
        gameTimer.stop();
    }
    public void endGame() {
        gameRunning = false;
        gameTimer.stop();
        if (aiTank != null) aiTank.saveLearnedData();
        player = null;
        aiTank = null;
        walls.clear();
    }
    public CollisionDetector getDetector() {
        return detector;
    }
}