package Config;

import java.io.*;
import java.util.Properties;

public class configTool {
    protected static final String CONFIG_FILE = "tankConfig.properties";
    protected static Properties props = new Properties();

    static {
        loadConfig();
    }

    protected static void loadConfig() {
        try (InputStream input = configTool.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
            } else {
                initDefaultConfig();
            }
        } catch (IOException ex) {
            System.err.println("无法加载配置文件，使用默认值");
            initDefaultConfig();
        }
    }

    protected static void initDefaultConfig() {
        props.setProperty("level", "1");
        props.setProperty("tankWhetherSelected", "false");
        props.setProperty("tankSelected", "null");
    }

    public static void saveConfig() {
        try (OutputStream output = new FileOutputStream(
                "src/main/resources/" + CONFIG_FILE)) {
            props.store(output, "Tank Configuration");
        } catch (IOException ex) {
            System.err.println("无法保存配置文件");
            ex.printStackTrace();
        }
    }

    // Getter和Setter方法
    public static int getLevel() {
        return Integer.parseInt(props.getProperty("level"));
    }

    public static boolean isTankSelected() {
        return Boolean.parseBoolean(props.getProperty("tankWhetherSelected"));
    }

    public static String getSelectedTank() {
        return props.getProperty("tankSelected");
    }

    public static void setTankSelection(String tankType) {
        props.setProperty("tankWhetherSelected", "true");
        props.setProperty("tankSelected", tankType);
        saveConfig();
    }

    public static void resetTankSelection() {
        props.setProperty("tankWhetherSelected", "false");
        props.setProperty("tankSelected", "null");
        saveConfig();
    }
}
