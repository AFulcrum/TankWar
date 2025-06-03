package Config;

import java.io.*;
import java.util.Properties;

public class ConfigTool {
    private static final String DATA_DIR = System.getProperty("user.dir")
            + File.separator + "Data";
    private static final String CONFIG_FILE = "TankConfig.properties";

    protected static Properties props = new Properties();

    static {
        loadConfig();
    }

    //加载配置文件
    protected static void loadConfig() {
        File configFile = new File(DATA_DIR, CONFIG_FILE);
        if (!configFile.exists()) {
            initDefaultConfig();
            saveConfig();
            return;
        }

        try (FileInputStream input = new FileInputStream(configFile)) {
            props.load(input);
        } catch (IOException ex) {
            System.err.println("无法加载配置文件: " + ex.getMessage());
            initDefaultConfig();
            saveConfig();
        }
    }
    private static void createDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (!created) {
                System.err.println("无法创建数据目录: " + DATA_DIR);
            } else {
                System.out.println("成功创建数据目录: " + DATA_DIR);
            }
        }
    }

    protected static void initDefaultConfig() {
        props.setProperty("level", "1");  //关卡数
        props.setProperty("ourScore", "0"); //我方分数
        props.setProperty("enemyScore", "0"); //敌方分数
        props.setProperty("beatNum","0");   //击败数
        props.setProperty("tankWhetherSelected","false"); //是否已经选择坦克
        props.setProperty("selectedTank", "null"); //选择的坦克
    }

    //保存配置文件
    public static void saveConfig() {
        File configDir = new File(DATA_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        try (OutputStream output = new FileOutputStream(
                DATA_DIR + File.separator + CONFIG_FILE)) {
            props.store(output, "Tank War Configuration");
        } catch (IOException ex) {
            System.err.println("无法保存配置文件: " + ex.getMessage());
        }
    }

    // Get方法
    public static int getOurScore() {
        return Integer.parseInt(props.getProperty("ourScore"));
    }
    public static int getEnemyScore() {
        return Integer.parseInt(props.getProperty("enemyScore"));
    }
    public static int getLevel() {
        return Integer.parseInt(props.getProperty("level"));
    }
    public static int getBeatNum() {
        return Integer.parseInt(props.getProperty("beatNum"));
    }

    public static boolean isTankSelected() {
        return Boolean.parseBoolean(props.getProperty("tankWhetherSelected"));
    }
    public static int getSelectedTank() {
        switch (props.getProperty("selectedTank")) {
            case "红坦克" -> {return 1;}
            case "蓝坦克" -> {return 2;}
            case "绿坦克" -> {return 3;}
            case "黄坦克" -> {return 4;}
            default -> {return 1;}  //默认为红
        }
    }

    //set方法
    public static void setLevel(String level) {
        props.setProperty("level", level);
        saveConfig();
    }
    public static void setOurScore(String score) {
        props.setProperty("ourScore", score);
        saveConfig();
    }
    public static void setEnemyScore(String score) {
        props.setProperty("enemyScore", score);
        saveConfig();
    }
    public static void setBeatNum(String num) {
        props.setProperty("beatNum", num);
        saveConfig();
    }

    public static void setTankSelection(String tankType) {
        props.setProperty("tankWhetherSelected", "true");
        props.setProperty("selectedTank", tankType);
        saveConfig();
    }

    public static void resetTankSelection() {
        props.setProperty("tankWhetherSelected", "false");
        props.setProperty("selectedTank", "null");
        saveConfig();
    }

    public static boolean isDebugMode() {
        String debugMode = props.getProperty("debugMode");
        return debugMode != null && debugMode.equalsIgnoreCase("true");
    }
}
