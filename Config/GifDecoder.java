package Config;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GifDecoder {
    private List<BufferedImage> frames = new ArrayList<>();
    
    public int read(InputStream is) {
        try {
            // 获取GIF图像读取器
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            ImageInputStream imageIn = ImageIO.createImageInputStream(is);
            reader.setInput(imageIn);
            
            // 获取GIF总帧数
            int frameCount = reader.getNumImages(true);
            
            // 读取每一帧
            for (int i = 0; i < frameCount; i++) {
                BufferedImage frame = reader.read(i);
                frames.add(frame);
            }
            
            return frameCount;
        } catch (IOException e) {
            System.err.println("读取GIF文件失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    public int getFrameCount() {
        return frames.size();
    }
    
    public BufferedImage getFrame(int n) {
        if (n >= 0 && n < frames.size()) {
            return frames.get(n);
        }
        return null;
    }
    
    /**
     * 获取指定大小的帧
     * @param n 帧索引
     * @param width 目标宽度
     * @param height 目标高度
     * @return 调整大小后的帧图像
     */
    public BufferedImage getScaledFrame(int n, int width, int height) {
        BufferedImage originalFrame = getFrame(n);
        if (originalFrame == null) return null;
        
        // 创建调整大小后的图像
        BufferedImage resizedFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedFrame.createGraphics();
        
        // 设置高质量缩放
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制调整大小后的图像
        g.drawImage(originalFrame, 0, 0, width, height, null);
        g.dispose();
        
        return resizedFrame;
    }
    
    /**
     * 获取所有帧的缩放版本
     * @param width 目标宽度
     * @param height 目标高度
     * @return 调整大小后的所有帧
     */
    public List<BufferedImage> getAllScaledFrames(int width, int height) {
        List<BufferedImage> scaledFrames = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            scaledFrames.add(getScaledFrame(i, width, height));
        }
        return scaledFrames;
    }
}
