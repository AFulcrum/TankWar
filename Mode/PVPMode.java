package Mode;

import Config.AITank;
import Config.EnemyTank;
import Config.PlayerTank;
import Config.SimpleCollisionDetector;
import InterFace.CollisionDetector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PVPMode extends JPanel {
    private PlayerTank player;
    private EnemyTank enemy;
    private Timer gameTimer;
    private boolean gameRunning = false;
    private CollisionDetector detector;

    public PVPMode(CollisionDetector collisionDetector) {
        this.detector = collisionDetector;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocusInWindow();

        // 创建玩家坦克和AI坦克
        player = new PlayerTank(100, 100, collisionDetector);
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
            }
        });
        repaint(); // 保证初始显示
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
        player.updateMovement(); // 每帧都根据按键状态移动
        player.updateBullets();
        // ...其他逻辑...
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
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
        } else {
            g.setColor(Color.RED);
            g.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());
        }
        player.drawBullets(g);
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

    public Object getDetector() {
        return detector;
    }
}