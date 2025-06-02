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
    private PlayerTank player;
    private ArrayList<AITank> aiTanks;
    private ArrayList<PVEWall> walls;
    private Timer gameTimer;
    private boolean gameRunning;
    private CollisionDetector detector;
    private JLabel levelLabel;
    private JLabel scoreLabel;
    private int currentLevel;
    private int playerScore;
    private int enemyScore;
    private static final int INITIAL_AI_TANKS = 2;
    private static final int SCORE_TO_WIN = 10;
    private static final int SCORE_TO_LOSE = 10;

    // 添加组件监听器以在尺寸变化时重新初始化墙体
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

        // 添加组件监听器
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (aiTanks != null && aiTanks.isEmpty()) {
                    spawnAITanks();
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
        aiTanks = new ArrayList<>();
        player = new PlayerTank(50, 50, detector);

        initWalls();
        spawnAITanks();
        updateDisplays();
    }

    private void initWalls() {
        Random random = new Random();
        int wallCount = 5 + currentLevel * 2;

        // 使用默认尺寸或等待组件尺寸可用
        int areaWidth = getWidth() <= 0 ? 800 : getWidth();
        int areaHeight = getHeight() <= 0 ? 600 : getHeight();

        for (int i = 0; i < wallCount; i++) {
            // 确保有40像素的边距
            int x = random.nextInt(Math.max(1, areaWidth - 40));
            int y = random.nextInt(Math.max(1, areaHeight - 40));
            walls.add(new PVEWall(x, y));
        }
    }

    private void spawnAITanks() {
        Random rand = new Random();
        int tankCount = INITIAL_AI_TANKS + (currentLevel - 1);

        // 使用默认尺寸或等待组件尺寸可用
        int areaWidth = getWidth() <= 0 ? 800 : getWidth();
        int areaHeight = getHeight() <= 0 ? 600 : getHeight();

        // 保证坦克不会生成在边缘
        int margin = 100;
        int spawnWidth = Math.max(1, areaWidth - 2 * margin);
        int spawnHeight = Math.max(1, areaHeight - 2 * margin);

        for (int i = 0; i < tankCount; i++) {
            int x = margin + rand.nextInt(spawnWidth);
            int y = margin + rand.nextInt(spawnHeight);
            AITank aiTank = new AITank(x, y, detector);
            aiTanks.add(aiTank);
        }
    }

    private void updateGame() {
        player.updateMovement();
        player.updateBullets();

        // 更新AI坦克
        for (AITank aiTank : aiTanks) {
            if (aiTank.isAlive()) {
                aiTank.updateAI(player, currentLevel);
            }
        }

        checkCollisions();
        checkScores();
        updateDisplays();
    }

    private void checkCollisions() {
        // 检查玩家子弹与AI坦克碰撞
        for (PlayerBullet bullet : player.getBullets()) {
            if (!bullet.isActive()) continue;

            for (AITank aiTank : aiTanks) {
                if (aiTank.isAlive() && bullet.getCollisionBounds().intersects(aiTank.getCollisionBounds())) {
                    bullet.deactivate();
                    aiTank.learn(false);
                    playerScore++;
                    ConfigTool.setOurScore(String.valueOf(playerScore));
                    break;
                }
            }
        }

        // 检查AI子弹与玩家碰撞
        for (AITank aiTank : aiTanks) {
            for (EnemyBullet bullet : aiTank.getBullets()) {
                if (bullet.isActive()) {
                    if (bullet.getCollisionBounds().intersects(player.getCollisionBounds())) {
                        bullet.deactivate();
                        aiTank.learn(true);
                        enemyScore++;
                        ConfigTool.setEnemyScore(String.valueOf(enemyScore));
                    }
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

        // 绘制墙体
        for (PVEWall wall : walls) {
            wall.draw(g);
        }

        // 绘制玩家
        if (player != null && player.getCurrentImage() != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            drawTank(g2d, player);
            player.drawBullets(g);
            g2d.dispose();
        }

        // 绘制AI坦克
        for (AITank aiTank : aiTanks) {
            if (aiTank.isAlive()) {
                Graphics2D g2d = (Graphics2D) g.create();
                drawTank(g2d, aiTank);
                aiTank.drawBullets(g);
                g2d.dispose();
            }
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

    // KeyListener实现
    @Override
    public void keyPressed(KeyEvent e) {
        if (player != null) {
            player.handleKeyPress(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (player != null) {
            player.handleKeyRelease(e.getKeyCode());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // 游戏控制方法
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
        // 保存AI学习数据
        for (AITank aiTank : aiTanks) {
            aiTank.saveLearnedData();
        }
        player = null;
        aiTanks.clear();
        walls.clear();
    }

    public CollisionDetector getDetector() {
        return detector;
    }
}