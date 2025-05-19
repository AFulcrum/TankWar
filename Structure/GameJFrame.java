package Structure;

import javax.swing.*;
import java.awt.*;
//整体框架
public class GameJFrame extends JFrame {
    private static GameJFrame instance;

    GameJFrame() {
        initFrame();
    }

    public static GameJFrame getInstance() {
        if (instance == null) {
            instance = new GameJFrame();
        }
        return instance;
    }

    public static void resetInstance() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }

    private void initFrame() {
        setTitle("TankWar");
        setSize(1000,800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
    }
}