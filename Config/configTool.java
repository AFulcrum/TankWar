package Config;

import java.io.*;
import java.util.Properties;

public class configTool {
    protected static final String CONFIG_FILE = "tankConfig.properties";
    protected static Properties props = new Properties();

    static {
        loadConfig();
    }

    //加载配置文件
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
        props.setProperty("selectedTank", "null");
    }

    //保存配置文件
    public static void saveConfig() {
        File configDir = new File("Config");
        if (!configDir.exists()) {
            configDir.mkdirs(); // 创建目录（包括所有不存在的父目录）
        }

        try (OutputStream output = new FileOutputStream(
                "Config/" + CONFIG_FILE)) {
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
        return props.getProperty("selectedTank");
    }

    public static void setTankSelection(String tankType) {
        props.setProperty("tankWhetherSelected", "true");
        props.setProperty("selectedTank", tankType);
        saveConfig();
    }

    public static void resetTankSelection() {
        props.setProperty("tankWhetherSelected", "false");
        props.setProperty("tankSelected", "null");
        saveConfig();
    }
}
