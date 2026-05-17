package org.mineUGC.core.message;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Messages {
    private final YamlConfiguration config;

    public Messages() {
        this.config = new YamlConfiguration();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
            if (in != null) {
                config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            // use empty config — keys will be returned as-is
        }
    }

    public String get(String key) {
        return config.getString(key);
    }

    public String get(String key, Object... args) {
        String msg = config.getString(key);
        if (msg == null) return key;
        return String.format(msg, args);
    }
}
