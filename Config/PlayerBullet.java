package Config;

import InterFace.Bullet;

import javax.swing.*;
import java.awt.*;

public class PlayerBullet implements Bullet {
    private int x;
    private int y;
    private int width = 10;
    private int height = 10;
    private int radius = 5; // 子弹半径
    private int speed = 15;
    private int damage = 1;
    private boolean active = true;
    private double angle; // 子弹飞行角度
    private Color bulletColor;
    private Image bulletImage;
    private int bounceCount = 0;
    private static final int MAX_BOUNCE = 66; // 最大反弹次数
    private static final int BULLET_LIFETIME = 10000; // 子弹最大存活时间10秒


    public PlayerBullet(int x, int y, double angle) {
        this.x = x - width/2; // 调整初始位置，使子弹中心对准发射点
        this.y = y - height/2;
        this.angle = angle;
        // 根据坦克类型设置不同颜色
        switch (ConfigTool.getSelectedTank()) {
            case 1:
                bulletColor = new Color(255, 69, 0); // 红色
                break;
            case 2:
                bulletColor = new Color(30, 144, 255); // 蓝色
                break;
            case 3:
                bulletColor = new Color(50, 205, 50); // 绿色
                break;
            case 4:
                bulletColor = new Color(255, 165, 0); // 橙色
                break;
            default:
                bulletColor = new Color(255, 69, 0); // 红色
        }
        // 添加子弹自动消失的定时器
        new Timer(BULLET_LIFETIME, e -> {
            deactivate();
            ((Timer)e.getSource()).stop();
        }).start();
    }


    @Override
    public int getSpeed() {
        return speed;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public void updatePosition() {
        if (!active) return;
        x += speed * Math.sin(angle);
        y -= speed * Math.cos(angle);
    }

    @Override
    public Rectangle getCollisionBounds() {
        if (!isActive()) return null;
        return new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void deactivate() {
        active = false;
    }
    @Override
    public void bounce() {
        if (!canBounce()) {
            deactivate();
            return;
        }

        // 获取当前移动方向
        double dx = Math.sin(angle);
        double dy = -Math.cos(angle);

        // 保存反弹前的位置，用于计算穿透深度
        int prevX = x;
        int prevY = y;

        // 判断反弹方向并确定更精确的后退距离
        if (Math.abs(dx) > Math.abs(dy)) {
            // 水平方向反弹
            angle = Math.PI - angle;

            // 计算水平穿透的深度并调整位置
            int penetrationDepth = Math.max(6, (int)(speed * Math.abs(dx) * 1.2));
            x -= (int)(Math.signum(dx) * penetrationDepth);
        } else {
            // 垂直方向反弹
            angle = -angle;

            // 计算垂直穿透的深度并调整位置
            int penetrationDepth = Math.max(6, (int)(speed * Math.abs(dy) * 1.2));
            y += (int)(Math.signum(dy) * penetrationDepth);
        }

        // 修正角度，保持在0-2π范围内
        angle = (angle + 2 * Math.PI) % (2 * Math.PI);

        // 增加反弹计数
        bounceCount++;
    }

    @Override
    public int getBounceCount() {
        return bounceCount;
    }

    @Override
    public boolean canBounce() {
        return bounceCount < MAX_BOUNCE;
    }


    @Override
    public void draw(Graphics g) {
        if (!active) return;

        Graphics2D g2d = (Graphics2D) g.create();
        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制主体
        g2d.setColor(bulletColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        g2d.dispose();
    }

    public double getAngle() {
        return angle;
    }
    public void setAngle(double newAngle) {
        this.angle = (newAngle + 2 * Math.PI) % (2 * Math.PI);
    }

    public void adjustPosition(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public double getX() {
        return x + width / 2.0; // 返回子弹中心的X坐标
    }

    public double getY() {
        return y + height / 2.0; // 返回子弹中心的Y坐标
    }

    /**
     * 获取子弹半径
     */
    public int getRadius() {
        return radius;
    }

    /**
     * 设置子弹位置（基于中心点）
     */
    public void setPosition(int centerX, int centerY) {
        this.x = centerX - width/2;
        this.y = centerY - height/2;
    }

    /**
     * 减少子弹速度（模拟能量损失）
     */
    public void decreaseSpeed(double factor) {
        this.speed = Math.max(5, (int)(this.speed * factor));
    }
}