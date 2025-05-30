package Config;

import InterFace.Bullet;

import javax.swing.*;
import java.awt.*;

public class EnemyBullet implements Bullet {
    private int x;
    private int y;
    private int width = 10;
    private int height = 10;
    private int speed = 1; // 比玩家子弹稍慢
    private int damage = 1;
    private boolean active = true;
    private double angle;
    private Image bulletImage;

    public EnemyBullet(int x, int y, double angle) {
        this.x = x - width/2;
        this.y = y - height/2;
        this.angle = angle;
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
        x += speed * Math.sin(angle);
        y -= speed * Math.cos(angle);
    }

    @Override
    public Rectangle getCollisionBounds() {
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

    public void draw(Graphics g) {
        if (!active || bulletImage == null) return;
        g.drawImage(bulletImage, x, y, null);
    }
}