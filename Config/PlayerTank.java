package Config;

import InterFace.CollisionDetector;
import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class PlayerTank extends AbstractTank {
    private final boolean[] keysPressed = new boolean[512];
    public static int health=3;
    private Image[] tankImages = new Image[2];
    private int tankType;
    private double angle = 0; // 当前朝向角度，0为向上，顺时针为正
    private boolean isMoving = false;
    private long lastImageSwitchTime = 0;
    private int currentImageIndex = 0;

    public PlayerTank(int x, int y, CollisionDetector collisionDetector) {
        super(x, y, 48, 48, 3, collisionDetector);
        this.tankType = ConfigTool.getSelectedTank();
        loadTankImage();
    }

    private void loadTankImage() {
        try {
            String[] paths = {
                    "/Images/TankImage/tank" + tankType + "/up1.png",
                    "/Images/TankImage/tank" + tankType + "/up2.png"
            };
            for (int i = 0; i < paths.length; i++) {
                java.net.URL url = getClass().getResource(paths[i]);
                if (url == null) {
                    System.err.println("图片不存在: " + paths[i]);
                    tankImages[i] = null;
                    continue;
                }
                ImageIcon icon = new ImageIcon(url);
                tankImages[i] = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            System.err.println("无法加载坦克图像: " + e.getMessage());
        }
    }

    // 处理键盘输入
    public void handleKeyPress(int keyCode) {
        keysPressed[keyCode] = true;
        updateMovement();
    }

    public void handleKeyRelease(int keyCode) {
        keysPressed[keyCode] = false;
        updateMovement();
    }

    public void updateMovement() {
        // 旋转角度,每次转动5度
        double rotateStep = Math.toRadians(5);

        if (keysPressed[KeyEvent.VK_LEFT]) {
            angle -= rotateStep;
        } else if (keysPressed[KeyEvent.VK_RIGHT]) {
            angle += rotateStep;
        }
        // 检测是否在移动
        boolean wasMoving = isMoving;
        isMoving = keysPressed[KeyEvent.VK_UP] || keysPressed[KeyEvent.VK_DOWN];

        // 如果开始移动或停止移动，重置动画计时
        if (isMoving != wasMoving) {
            lastImageSwitchTime = System.currentTimeMillis();
        }
        // 移动
        int moveSpeed = 0;
        if (keysPressed[KeyEvent.VK_UP]) {
            moveSpeed = speed;
        } else if (keysPressed[KeyEvent.VK_DOWN]) {
            moveSpeed = -speed;
        }

        if (moveSpeed != 0) {
            int newX = (int) (x + moveSpeed * Math.sin(angle));
            int newY = (int) (y - moveSpeed * Math.cos(angle));
            if (checkCollision(newX, newY)) {
                x = newX;
                y = newY;
            }
        }

        if (keysPressed[KeyEvent.VK_SPACE]) {
            fire();
        }
        if (keysPressed[KeyEvent.VK_X]) {
            useSkill();
        }
    }
    public Image getCurrentImage() {
        // 如果不移动或没有图片，返回第一张图片
        if (!isMoving || tankImages[0] == null) {
            return tankImages[0];
        }

        // 动画切换逻辑 - 每100毫秒切换一次图片
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastImageSwitchTime > 100) {
            currentImageIndex = (currentImageIndex + 1) % 2;
            lastImageSwitchTime = currentTime;
        }

        return tankImages[currentImageIndex];
    }

    @Override
    public void fire() {
        System.out.println("发射子弹");
        // ...子弹逻辑...
    }

    @Override
    public void useSkill() {
        int originalSpeed = speed;
        speed *= 2;
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        speed = originalSpeed;
                    }
                },
                3000
        );
    }

    @Override
    public int getDirection() {
        // 将角度标准化到0-2π范围
        double normalizedAngle = (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);

        // 将角度转换为方向（0:上, 1:右, 2:下, 3:左）
        if (normalizedAngle >= Math.PI/4 && normalizedAngle < 3*Math.PI/4) {
            return 1; // 右
        } else if (normalizedAngle >= 3*Math.PI/4 && normalizedAngle < 5*Math.PI/4) {
            return 2; // 下
        } else if (normalizedAngle >= 5*Math.PI/4 && normalizedAngle < 7*Math.PI/4) {
            return 3; // 左
        } else {
            return 0; // 上
        }
    }

    public static int getHealth(){
        return health;
    }


    public double getAngle() {
        return angle;
    }

    public boolean checkCollision(int newX, int newY) {
        return !collisionDetector.isColliding(newX, newY, width, height);
    }
}