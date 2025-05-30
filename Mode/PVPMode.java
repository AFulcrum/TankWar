package Mode;

import Config.*;
import InterFace.CollisionDetector;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class PVPMode extends JPanel {
    private PlayerTank player;
    private EnemyTank enemy;
    private Timer gameTimer;
    private boolean gameRunning = false;
    private CollisionDetector detector;
    private java.util.List<Wall> walls = new ArrayList<>(); // 添加墙体列表


    public PVPMode(CollisionDetector collisionDetector) {
        this.detector = collisionDetector;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocusInWindow();

        // 创建玩家坦克和AI坦克
        player = new PlayerTank(100, 100, collisionDetector);
        enemy = new EnemyTank(300, 300, collisionDetector);
        // 初始化墙体
        initWalls();
        // 更新碰撞检测器
        if (detector instanceof SimpleCollisionDetector) {
            ((SimpleCollisionDetector) detector).setWalls(walls);
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
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateCollisionBoundary();
                // 重新初始化墙体
                walls.clear();
                initWalls();
                if (detector instanceof SimpleCollisionDetector) {
                    ((SimpleCollisionDetector) detector).setWalls(walls);
                }
            }
        });
        repaint(); // 保证初始显示
    }
    // 初始化墙体
    private void initWalls() {
        // 只有当游戏区域大小足够时才初始化墙体
        if (getWidth() > 0 && getHeight() > 0) {
            // 添加边界墙体
            walls.addAll(Wall.createBoundaryWalls(getWidth(), getHeight()));
            // 添加随机墙体
            walls.addAll(Wall.createRandomWalls(getWidth(), getHeight(), 10));
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

    private void updateGame() {
        if (!gameRunning) return;
        player.updateMovement(); // 更新玩家坦克
        player.updateBullets();// 更新玩家子弹
        enemy.update();// 更新敌方坦克(包括移动和射击)
        enemy.updateBullets();// 更新敌方子弹
        // 检查子弹碰撞
        checkBulletCollisions();

        repaint();

    }
    private void checkBulletCollisions() {
        if (player == null || enemy == null || player.getBullets() == null) {
            return;
        }
        // 检查玩家子弹是否击中敌方坦克
        for (PlayerBullet bullet : player.getBullets()) {
            if (bullet != null && bullet.isActive() && enemy.isAlive()) {
                Rectangle bulletBounds = bullet.getCollisionBounds();
                Rectangle enemyBounds = enemy.getCollisionBounds();

                if (bulletBounds != null && enemyBounds != null &&
                        bulletBounds.intersects(enemyBounds)) {

                    bullet.deactivate();
                    enemy.takeDamage(bullet.getDamage());

                    if (!enemy.isAlive()) {
                        // 敌方坦克被摧毁，增加得分
                        ConfigTool.setBeatNum(String.valueOf(ConfigTool.getBeatNum() + 1));
                        ConfigTool.saveConfig();

                        // 1秒后在随机位置重生
                        Timer respawnTimer = new Timer(1000, e -> {
                            respawnEnemy();
                            ((Timer)e.getSource()).stop();
                        });
                        respawnTimer.setRepeats(false);
                        respawnTimer.start();
                    }
                }
            }
        }
    }

    private void respawnEnemy() {
        Random rand = new Random();
        int newX = rand.nextInt(getWidth() - enemy.getWidth());
        int newY = rand.nextInt(getHeight() - enemy.getHeight());

        enemy = new EnemyTank(newX, newY, detector);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
//        g.setColor(Color.WHITE);
        // 清空背景
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        // 绘制墙体
        for (Wall wall : walls) {
            wall.draw(g);
        }
        // 绘制玩家坦克
        if (player.getCurrentImage() != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            int px = player.getX();
            int py = player.getY();
            int pw = player.getWidth();
            int ph = player.getHeight();
            double angle = player.getAngle();
            // 以坦克中心为旋转中心
            g2d.translate(px + pw / 2, py + ph / 2);
            g2d.rotate(angle);
            g2d.drawImage(player.getCurrentImage(), -pw / 2, -ph / 2, pw, ph, null);
            g2d.dispose();
        }
        // 绘制敌方坦克
        if (enemy.isAlive() && enemy.getTankImage() != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            int ex = enemy.getX();
            int ey = enemy.getY();
            int ew = enemy.getWidth();
            int eh = enemy.getHeight();
            double angle = enemy.getAngle();
            g2d.translate(ex + ew / 2, ey + eh / 2);
            g2d.rotate(angle);
            g2d.drawImage(enemy.getTankImage(), -ew / 2, -eh / 2, ew, eh, null);
            g2d.dispose();
        }
        // 绘制子弹
        player.drawBullets(g);
        enemy.drawBullets(g);
    }

    private void drawTank(Graphics g, PlayerTank tank, Color color) {
        g.setColor(color);
        g.fillRect(tank.getX(), tank.getY(), tank.getWidth(), tank.getHeight());

        // 绘制炮管方向
        int centerX = tank.getX() + tank.getWidth()/2;
        int centerY = tank.getY() + tank.getHeight()/2;
        switch (tank.getDirection()) {
            case 0: // 上
                g.drawLine(centerX, centerY, centerX, centerY - 20);
                break;
            case 1: // 右
                g.drawLine(centerX, centerY, centerX + 20, centerY);
                break;
            case 2: // 下
                g.drawLine(centerX, centerY, centerX, centerY + 20);
                break;
            case 3: // 左
                g.drawLine(centerX, centerY, centerX - 20, centerY);
                break;
        }
    }


    public void startGame() {
        gameRunning = true;
        gameTimer.start();
    }

    public void stopGame() {
        gameRunning = false;
    }

    public void endGame() {
        gameRunning = false;
        gameTimer.stop();
        player = null;
        enemy = null;
    }

//    public Object getDetector() {
//        return detector;
//    }
    public CollisionDetector getDetector() {
        return detector;
    }

}