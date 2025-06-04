package Mode;

import Config.*;
import InterFace.CollisionDetector;
import Structure.PVPWall;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
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

    // 在updateGame方法中添加检查
    private void updateGame() {
        if (!gameRunning) return;

        player.updateMovement(); // 更新玩家坦克
        player.updateBullets(); // 更新玩家子弹

        // 更新所有敌方坦克
        for (EnemyTank enemy : enemies) {
            enemy.update();      // 更新敌方坦克(包括移动和射击)
            enemy.updateBullets(); // 更新敌方子弹
        }
        // 检查子弹与墙体/边界的碰撞（新增）
        checkBulletWallCollisions();
        // 检查子弹与坦克的碰撞
        checkBulletCollisions();

        repaint();
    }

    // 重新定位玩家坦克到随机位置
    private void repositionPlayerTank() {
        if (getWidth() <= 100 || getHeight() <= 100) return;

        Random rand = new Random();
        int playerX = rand.nextInt(Math.max(1, getWidth() - 100));
        int playerY = rand.nextInt(Math.max(1, getHeight() - 100));

        player.setPosition(playerX, playerY);
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
    // 修改检查墙体碰撞的方法
    private void checkBulletWallCollisions() {
        // 检查玩家子弹与墙体和边界的碰撞
        for (PlayerBullet bullet : player.getBullets()) {
            if (!bullet.isActive()) continue;

            Rectangle bulletBounds = bullet.getCollisionBounds();
            if (bulletBounds == null) continue;
            // 获取子弹当前方向
            double angle = bullet.getAngle();
            double dx = Math.sin(angle);
            double dy = -Math.cos(angle);

            // 预测下一位置
            Rectangle nextBulletPos = new Rectangle(
                    bulletBounds.x + (int)(dx * 3),
                    bulletBounds.y - (int)(dy * 3),
                    bulletBounds.width,
                    bulletBounds.height
            );

            boolean hitWall = false;
            boolean isHorizontalCollision = false;
            // 检查是否与墙体碰撞
            for (PVPWall PVPWall : PVPWalls) {
                Rectangle wallBounds = PVPWall.getCollisionBounds();

                // 检查当前位置是否已经与墙重叠(可能已经穿模)
                if (bulletBounds.intersects(wallBounds)) {
                    hitWall = true;
                    // 确定碰撞方向
                    int overlapLeft = (wallBounds.x + wallBounds.width) - bulletBounds.x;
                    int overlapRight = (bulletBounds.x + bulletBounds.width) - wallBounds.x;
                    int overlapTop = (wallBounds.y + wallBounds.height) - bulletBounds.y;
                    int overlapBottom = (bulletBounds.y + bulletBounds.height) - wallBounds.y;
                    // 找出最小重叠方向
                    int minOverlap = Math.min(
                            Math.min(overlapLeft, overlapRight),
                            Math.min(overlapTop, overlapBottom)
                    );
                    // 确定主要碰撞方向
                    if (minOverlap == overlapLeft || minOverlap == overlapRight) {
                        isHorizontalCollision = true;
                    }
                    break;
                }
                // 检查下一位置是否会与墙重叠(即将碰撞)
                if (!hitWall && nextBulletPos.intersects(wallBounds)) {
                    hitWall = true;
                    // 通过移动方向判断碰撞面
                    if (Math.abs(dx) > Math.abs(dy)) {
                        isHorizontalCollision = true;
                    }
                    break;
                }
            }
            // 检查是否碰到边界
            if (!hitWall) {
                int gameWidth = getWidth();
                int gameHeight = getHeight();

                if (bulletBounds.x <= 0 || bulletBounds.x + bulletBounds.width >= gameWidth) {
                    hitWall = true;
                    isHorizontalCollision = true;
                } else if (bulletBounds.y <= 0 || bulletBounds.y + bulletBounds.height >= gameHeight) {
                    hitWall = true;
                    isHorizontalCollision = false;
                }
            }
            // 处理碰撞结果
            if (hitWall) {
                if (bullet.canBounce()) {
                    // 根据碰撞面设置反弹角度
                    handleBulletBounce(bullet, isHorizontalCollision);
                } else {
                    bullet.deactivate();
                }
            }
        }
        // 对敌方子弹执行相同的碰撞检测
        for (EnemyTank enemy : enemies) {
            for (EnemyBullet bullet : enemy.getBullets()) {
                if (!bullet.isActive()) continue;

                Rectangle bulletBounds = bullet.getCollisionBounds();
                if (bulletBounds == null) continue;

                // 获取子弹当前方向
                double angle = bullet.getAngle();
                double dx = Math.sin(angle);
                double dy = -Math.cos(angle);

                // 预测下一位置
                Rectangle nextBulletPos = new Rectangle(
                        bulletBounds.x + (int)(dx * 3),
                        bulletBounds.y - (int)(dy * 3),
                        bulletBounds.width,
                        bulletBounds.height
                );

                boolean hitWall = false;
                boolean isHorizontalCollision = false;

                // 检查墙体碰撞
                for (PVPWall PVPWall : PVPWalls) {
                    Rectangle wallBounds = PVPWall.getCollisionBounds();

                    if (bulletBounds.intersects(wallBounds)) {
                        hitWall = true;

                        int overlapLeft = (wallBounds.x + wallBounds.width) - bulletBounds.x;
                        int overlapRight = (bulletBounds.x + bulletBounds.width) - wallBounds.x;
                        int overlapTop = (wallBounds.y + wallBounds.height) - bulletBounds.y;
                        int overlapBottom = (bulletBounds.y + bulletBounds.height) - wallBounds.y;

                        int minOverlap = Math.min(
                                Math.min(overlapLeft, overlapRight),
                                Math.min(overlapTop, overlapBottom)
                        );

                        if (minOverlap == overlapLeft || minOverlap == overlapRight) {
                            isHorizontalCollision = true;
                        }
                        break;
                    }

                    if (!hitWall && nextBulletPos.intersects(wallBounds)) {
                        hitWall = true;

                        if (Math.abs(dx) > Math.abs(dy)) {
                            isHorizontalCollision = true;
                        }
                        break;
                    }
                }

                // 检查边界碰撞
                if (!hitWall) {
                    int gameWidth = getWidth();
                    int gameHeight = getHeight();

                    if (bulletBounds.x <= 0 || bulletBounds.x + bulletBounds.width >= gameWidth) {
                        hitWall = true;
                        isHorizontalCollision = true;
                    } else if (bulletBounds.y <= 0 || bulletBounds.y + bulletBounds.height >= gameHeight) {
                        hitWall = true;
                        isHorizontalCollision = false;
                    }
                }

                // 处理碰撞结果
                if (hitWall) {
                    if (bullet.canBounce()) {
                        handleEnemyBulletBounce(bullet, isHorizontalCollision);
                    } else {
                        bullet.deactivate();
                    }
                }
            }
        }
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
                            enemy.takeDamage(bullet.getDamage());

                            if (!enemy.isAlive()) {
                                // 敌方坦克被摧毁，增加得分
                                ConfigTool.setBeatNum(String.valueOf(ConfigTool.getBeatNum() + 1));
                                ConfigTool.saveConfig();
                                updateDisplays();

                                // 移除被摧毁的敌方坦克
                                enemies.remove(i);
                                i--;

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
                        bullet.deactivate(); // 同样，击中坦克直接消失
                        player.takeDamage(bullet.getDamage());
                        updateDisplays();
                        // 检查游戏是否结束
                        if (!player.isAlive()) {
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
        JOptionPane.showMessageDialog(this,
                "游戏结束！\n击败敌方坦克数: " + ConfigTool.getBeatNum(),
                "游戏结束",
                JOptionPane.INFORMATION_MESSAGE);
        endGame();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 清空背景
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        // 绘制墙体
        for (PVPWall PVPWall : PVPWalls) {
            PVPWall.draw(g);
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

        // 绘制所有敌方坦克
        for (EnemyTank enemy : enemies) {
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
        }

        // 绘制子弹
        player.drawBullets(g);
        for (EnemyTank enemy : enemies) {
            enemy.drawBullets(g);
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
        enemies.clear();
    }

    public CollisionDetector getDetector() {
        return detector;
    }

}