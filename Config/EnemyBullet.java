package Config;

import InterFace.Bullet;

import javax.swing.*;
import java.awt.*;

public class EnemyBullet implements Bullet {
    private int x;
    private int y;
    private int width = 10;
    private int height = 10;
    private int speed = 10; // 比玩家子弹稍慢
    private int damage = 1;
    private boolean active = true;
    private double angle;
    private Image bulletImage;
    private double dx, dy; // 方向向量
    private int bounceCount = 0;
    private static final int MAX_BOUNCE = 66; // 最大反弹次数

    public EnemyBullet(int x, int y, double angle) {
        this.x = x - width/2;
        this.y = y - height/2;
        this.angle = angle;
        this.dx = Math.sin(angle);
        this.dy = -Math.cos(angle);
        loadBulletImage();
    }

    private void loadBulletImage() {
        try {
            String path = "/Images/Bullet/bullet5.png";
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                path = "/Images/Bullet/bullet5.png";
                url = getClass().getResource(path);
                if (url == null) {
                    System.err.println("子弹图片不存在: " + path);
                    return;
                }
            }
            ImageIcon icon = new ImageIcon(url);
            bulletImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.err.println("无法加载子弹图像: " + e.getMessage());
        }
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
        return new Rectangle(x, y, width, height);
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
        double penetrationFactor = 1.2;

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

    public void draw(Graphics g) {
        if (!active || bulletImage == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(x + width/2, y + height/2);
        g2d.rotate(angle);
        g2d.drawImage(bulletImage, -width/2, -height/2, null);
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