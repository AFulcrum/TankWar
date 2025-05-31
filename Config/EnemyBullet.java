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
    private static final int MAX_BOUNCE = 100; // 最大反弹次数

    public EnemyBullet(int x, int y, double angle) {
        this.x = x - width/2;
        this.y = y - height/2;
        this.angle = angle;
        this.dx = speed * Math.sin(angle);
        this.dy = speed * Math.cos(angle);
        loadBulletImage();
    }

    private void loadBulletImage() {
        try {
            String path = "/Images/Bullet/Bullet5.png";
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("子弹图片不存在: " + path);
                return;
            }
            ImageIcon icon = new ImageIcon(url);
            bulletImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.err.println("无法加载子弹图像: " + e.getMessage());
        }
    }

    // 实现Bullet接口方法，与PlayerBullet类似
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
        x += dx;
        y -= dy;
    }

    @Override
    public Rectangle getCollisionBounds() {
        if (!active) return null;
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

        // 判断反弹方向（简化版）
        if (Math.abs(dx) > Math.abs(dy)) {
            // 水平方向反弹
            dx = -dx;
            angle = Math.PI - angle;
        } else {
            // 垂直方向反弹
            dy = -dy;
            angle = -angle;
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
        g.drawImage(bulletImage, x, y, null);
    }
}