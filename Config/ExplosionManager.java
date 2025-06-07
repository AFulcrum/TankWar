package Config;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExplosionManager {
    private static ExplosionManager instance;
    private List<Explosion> activeExplosions = new ArrayList<>();
    private List<Image> explosionFrames = new ArrayList<>();
    private boolean framesLoaded = false;
    
    // 单例模式
    public static ExplosionManager getInstance() {
        if (instance == null) {
            instance = new ExplosionManager();
        }
        return instance;
    }
    
    private ExplosionManager() {
        loadExplosionFrames();
    }
    
    // 加载爆炸动画帧
    private void loadExplosionFrames() {
        try {
            URL url = getClass().getResource("/Images/explotion.gif");
            if (url == null) {
                System.err.println("找不到爆炸GIF文件");
                return;
            }
            
            // 使用GifDecoder解析GIF帧
            GifDecoder decoder = new GifDecoder();
            decoder.read(url.openStream());
            
            // 指定一个标准大小
            int standardSize = 66;

            System.out.println("加载爆炸GIF，共" + decoder.getFrameCount() + "帧");
            // 使用缩放版本而不是原始帧
            List<BufferedImage> scaledFrames = decoder.getAllScaledFrames(standardSize, standardSize);
            explosionFrames.addAll(scaledFrames);
            
            if (!explosionFrames.isEmpty()) {
                Image firstFrame = explosionFrames.get(0);
                System.out.println("第一帧大小: " + firstFrame.getWidth(null) + "x" + firstFrame.getHeight(null));
            }

            framesLoaded = true;
        } catch (Exception e) {
            System.err.println("加载爆炸动画失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 在指定位置创建爆炸
    public void createExplosion(int x, int y, int size) {
        if (!framesLoaded || explosionFrames.isEmpty()) {
            System.out.println("爆炸帧未加载，无法创建爆炸效果");
            return;
        }
        
        System.out.println("请求的爆炸大小: " + size);

        Explosion explosion = new Explosion(x, y, size);
        activeExplosions.add(explosion);
        System.out.println("创建爆炸效果，位置: " + x + "," + y + ", 大小: " + size);
    }
    
    // 更新所有爆炸
    public void update() {
        Iterator<Explosion> iterator = activeExplosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update();
            if (explosion.isFinished()) {
                iterator.remove();
            }
        }
    }
    
    // 绘制所有爆炸
    public void draw(Graphics g) {
        for (Explosion explosion : activeExplosions) {
            explosion.draw(g);
        }
    }
    
    // 内部爆炸类
    private class Explosion {
        private int x, y;
        private int size;
        private int currentFrame = 0;
        private long lastFrameTime;
        private static final int FRAME_DURATION = 200; // 每帧持续200毫秒
        
        public Explosion(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.lastFrameTime = System.currentTimeMillis();
        }
        
        public void update() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime > FRAME_DURATION) {
                currentFrame++;
                lastFrameTime = currentTime;
            }
        }
        
        public boolean isFinished() {
            return currentFrame >= explosionFrames.size();
        }
        
        public void draw(Graphics g) {
            if (isFinished() || explosionFrames.isEmpty()) return;
            
            // 获取当前帧
            Image frame = explosionFrames.get(currentFrame);
            int frameWidth = frame.getWidth(null);
            int frameHeight = frame.getHeight(null);
            
            // 不进行拉伸，而是居中显示
            int drawX = x - frameWidth / 2;
            int drawY = y - frameHeight / 2;
            
            // 绘制主爆炸图像 - 不指定宽高，使用原始大小
            g.drawImage(frame, drawX, drawY, null);
        }
    }
}
