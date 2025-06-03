package Config;

import InterFace.Bullet;

import javax.swing.*;
import java.awt.*;

public class EnemyBullet implements Bullet {
    public static final Object DEFAULT_SPEED = 15;
    private int x;
    private int y;
    private int width = 10;
    private int height = 10;
    private int radius = 5; // 子弹半径
    private int speed = 10; // 比玩家子弹稍慢
    private int damage = 1;
    private boolean active = true;
    private double angle;
    private Color bulletColor;
    private double dx, dy; // 方向向量
    private int bounceCount = 0;
    private static final int MAX_BOUNCE = 66; // 最大反弹次数

    public EnemyBullet(int x, int y, double angle) {
        this.x = x - width/2;
        this.y = y - height/2;
        this.angle = angle;
        this.dx = Math.sin(angle);
        this.dy = -Math.cos(angle);
        bulletColor = new Color(154, 154, 154, 255); // 半透明红色
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
        x += speed * dx;
        y += speed * dy;
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
        double penetrationFactor = 1.5;

        // 判断反弹方向并确定更精确的后退距离
        if (Math.abs(dx) > Math.abs(dy)) {
            // 水平方向反弹
            angle = Math.PI - angle;
            dx = -dx;

            // 计算水平穿透的深度并调整位置
            int penetrationDepth = Math.max(6, (int)(speed * Math.abs(dx) * penetrationFactor));
            x -= (int)(Math.signum(dx) * penetrationDepth);
        } else {
            // 垂直方向反弹
            angle = -angle;
            dy = -dy;

            // 计算垂直穿透的深度并调整位置
            int penetrationDepth = Math.max(6, (int)(speed * Math.abs(dy) * penetrationFactor));
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
        // 更新方向向量
        this.dx = Math.sin(this.angle);
        this.dy = -Math.cos(this.angle);
    }

    public void adjustPosition(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }
}