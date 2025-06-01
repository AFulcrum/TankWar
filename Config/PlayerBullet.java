package Config;

import InterFace.Bullet;

import javax.swing.*;
import java.awt.*;

public class PlayerBullet implements Bullet {
    private int x;
    private int y;
    private int width = 10;
    private int height = 10;
    private int speed = 15;
    private int damage = 1;
    private boolean active = true;
    private double angle; // 子弹飞行角度
    private Image bulletImage;
    private int bounceCount = 0;
    private static final int MAX_BOUNCE = 66; // 最大反弹次数
    private static final int BULLET_LIFETIME = 10000; // 子弹最大存活时间10秒


    public PlayerBullet(int x, int y, double angle) {
        this.x = x - width/2; // 调整初始位置，使子弹中心对准发射点
        this.y = y - height/2;
        this.angle = angle;
        loadBulletImage();
        // 添加子弹自动消失的定时器
        new Timer(BULLET_LIFETIME, e -> {
            deactivate();
            ((Timer)e.getSource()).stop();
        }).start();
    }

    private void loadBulletImage() {
        try {
            String path = "/Images/Bullet/bullet" + ConfigTool.getSelectedTank() + ".png";
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                path = "/Images/Bullet/bullet1.png";
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
        x += speed * Math.sin(angle);
        y -= speed * Math.cos(angle);
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

    public void draw(Graphics g) {
        if (!active || bulletImage == null) return;
        g.drawImage(bulletImage, x, y, null);
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
}