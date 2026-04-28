package team.avion.common.localization;

import java.util.Locale;
import java.util.Set;

public final class PluginLocalization {
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String SYSTEM_LANGUAGE = "system";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "ru", "nl", "ja");

    private PluginLocalization() {
    }

    public static String resolveConfiguredLanguage(String configuredLanguage) {
        if (configuredLanguage == null || configuredLanguage.isBlank()
                || SYSTEM_LANGUAGE.equalsIgnoreCase(configuredLanguage)) {
            return resolveSystemLanguage();
        }

        String normalized = configuredLanguage.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE;
    }

    public static String resolveSystemLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        String normalized = language.toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE;
    }

    public static String normalizeConfigLanguage(String configuredLanguage) {
        if (configuredLanguage == null || configuredLanguage.isBlank()) {
            return SYSTEM_LANGUAGE;
        }

        String normalized = configuredLanguage.trim().toLowerCase(Locale.ROOT);
        if (SYSTEM_LANGUAGE.equals(normalized)) {
            return SYSTEM_LANGUAGE;
        }
        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE;
    }
}
