package src.Config;

import src.InterFace.Bullet;

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
    private static final int MAX_BOUNCE = 6; // 最大反弹次数
    private double travelDistance = 0;
    private double minCollisionDistance = 20;

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

    public void setMinCollisionDistance(double distance) {
        this.minCollisionDistance = distance;
    }

    @Override
    public void updatePosition() {
        if (!active) return;

        double oldX = x;
        double oldY = y;

        // 对于高速子弹使用分步移动防止穿墙
        if (speed > 10) {
            // 将移动分为多个小步骤，确保不会在单帧内跳过细墙
            int steps = (int)Math.ceil(speed / 8.0);
            double stepSize = speed / steps;
            
            for (int i = 0; i < steps; i++) {
                // 增量更新位置
                x += Math.cos(angle) * stepSize;
                y += Math.sin(angle) * stepSize;
            }
        } else {
            // 正常速度直接更新
            x += Math.cos(angle) * speed;
            y += Math.sin(angle) * speed;
        }

        // 计算移动距离
        double moveDist = Math.sqrt(Math.pow(x-oldX, 2) + Math.pow(y-oldY, 2));
        travelDistance += moveDist;
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
        
        // 增加穿透系数，防止卡墙
        double penetrationFactor = 1.8;

        // 判断反弹方向并确定更精确的后退距离
        if (Math.abs(dx) > Math.abs(dy)) {
            // 水平方向反弹
            angle = Math.PI - angle;
            this.dx = -dx;

            // 增加穿透距离计算，防止卡墙
            int penetrationDepth = Math.max(10, (int)(speed * Math.abs(dx) * penetrationFactor));
            x -= (int)(Math.signum(dx) * penetrationDepth);
        } else {
            // 垂直方向反弹
            angle = -angle;
            this.dy = -dy;

            // 增加穿透距离计算，防止卡墙
            int penetrationDepth = Math.max(10, (int)(speed * Math.abs(dy) * penetrationFactor));
            y += (int)(Math.signum(dy) * penetrationDepth);
        }

        // 修正角度，保持在0-2π范围内
        angle = (angle + 2 * Math.PI) % (2 * Math.PI);

        // 增加反弹计数
        bounceCount++;
        
        // 每次反弹略微减速，模拟能量损失
        speed = Math.max(5, (int)(speed * 0.95));
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

    public boolean canCollide() {
        return active && travelDistance >= minCollisionDistance;
    }

    // 添加获取子弹已飞行距离的方法
    public double getTravelDistance() {
        return travelDistance;
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