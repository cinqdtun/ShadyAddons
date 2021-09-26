package cheaters.get.banned.config;

import cheaters.get.banned.Shady;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfigLogic {
    
    private static String fileName = "config/ShadyAddons.cfg";
    private static HashMap<String, String> hashedSettings = new HashMap<>();

    private static HashMap<String, String> generateHashes(ArrayList<Setting> settings) {
        HashMap<String, String> output = new HashMap<>();
        for(Setting setting : settings) {
            output.put(DigestUtils.md5Hex(setting.name), setting.name);
        }
        return output;
    }

    public static ArrayList<Setting> collect(Class<Config> instance) {
        Field[] fields = instance.getDeclaredFields();
        ArrayList<Setting> settings = new ArrayList<>();

        for(Field field : fields) {
            Property annotation = field.getAnnotation(Property.class);
            if(annotation != null) {
                settings.add(new Setting(
                        annotation.value(),
                        annotation.hidden(),
                        annotation.parent().equals("") ? null : annotation.parent(),
                        annotation.boundTo().equals("") ? null : annotation.boundTo(),
                        annotation.tooltip().equals("") ? null : annotation.tooltip(),
                        annotation.type(),
                        field
                ));
            }
        }

        for(int i = 0; i < settings.size(); i++) {
            Setting newSetting = settings.get(i);
            newSetting.children = newSetting.getChildren(settings);
            settings.set(i, newSetting);
        }

        hashedSettings = generateHashes(settings);
        return settings;
    }

    public static Setting getSetting(String name) {
        for(Setting setting : Shady.settings) {
            if(setting.name.equals(name)) return setting;
        }
        return null;
    }

    public static void save() {
        try {
            HashMap<String, Boolean> convertedSettings = new HashMap<>();
            for(Setting setting : Shady.settings) {
                String settingHash = DigestUtils.md5Hex(setting.name);
                convertedSettings.put(settingHash, setting.enabled());
            }
            String json = new Gson().toJson(convertedSettings);
            Files.write(Paths.get(fileName), json.getBytes(StandardCharsets.UTF_8));
        } catch(Exception error) {
            System.out.println("Error while saving config file");
            error.printStackTrace();
        }
    }

    public static void load() {
        try {
            File file = new File(fileName);
            if(file.exists()) {
                Reader reader = Files.newBufferedReader(Paths.get(fileName));
                Type type = new TypeToken<HashMap<String, Boolean>>(){}.getType();

                HashMap<String, Boolean> settingsToProcess = new Gson().fromJson(reader, type);

                for(Map.Entry<String, Boolean> settingToProcess : settingsToProcess.entrySet()) {
                    if(hashedSettings.containsKey(settingToProcess.getKey())) {
                        Setting settingToAdd = getSetting(hashedSettings.get(settingToProcess.getKey()));
                        if(settingToAdd != null) settingToAdd.set(settingToProcess.getValue());
                    }
                }
            }
        } catch(Exception error) {
            System.out.println("Error while loading config file");
            error.printStackTrace();
        }
    }

}