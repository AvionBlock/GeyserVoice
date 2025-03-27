package io.greitan.avion.paper.utils;

import org.bukkit.configuration.file.YamlConfiguration;

import io.greitan.avion.paper.GeyserVoice;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Language {
    private static final Map<String, YamlConfiguration> languageConfigs = new HashMap<>();
    private static String defaultLanguage = "en";

    public static void init(GeyserVoice plugin) {
        File languageFolder = new File(plugin.getDataFolder(), "locale");

        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
            plugin.saveResource("locale/en.yml");
            plugin.saveResource("locale/ru.yml");
            plugin.saveResource("locale/nl.yml");
            plugin.saveResource("locale/ja.yml");
        }

        loadLanguages(languageFolder.getAbsolutePath());
    }

    private static void loadLanguages(String pluginFolder) {
        File languageFolder = new File(pluginFolder);

        if (languageFolder.exists() && languageFolder.isDirectory()) {
            for (File file : languageFolder.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    String language = file.getName().replace(".yml", "");
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    languageConfigs.put(language, config);
                }
            }
        }
    }

    public static String getMessage(String language, String key) {
        if (languageConfigs.containsKey(language)) {
            YamlConfiguration config = languageConfigs.get(language);
            if (config.contains("messages." + key)) {
                return config.getString("messages." + key);
            }
        }
        return languageConfigs.get(defaultLanguage).getString("messages." + key);
    }
}
