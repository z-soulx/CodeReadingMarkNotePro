package jp.kitabatakep.intellij.plugins.codereadingnote;

import jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * Resource bundle for Code Reading Note Pro plugin internationalization
 * Supports custom language selection independent of IDE language
 */
public final class CodeReadingNoteBundle {
    
    private static final String BUNDLE = "messages.CodeReadingNoteBundle";
    
    // Use a custom control to bypass ResourceBundle cache and handle UTF-8 encoding
    private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
        @Override
        public long getTimeToLive(String baseName, Locale locale) {
            // Return 0 to disable caching
            return ResourceBundle.Control.TTL_DONT_CACHE;
        }
        
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                       ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            
            if (!"java.properties".equals(format)) {
                return null;
            }
            
            // Load properties files with UTF-8 encoding instead of ISO-8859-1
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            
            InputStream stream = null;
            if (reload) {
                java.net.URL url = loader.getResource(resourceName);
                if (url != null) {
                    java.net.URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            
            if (stream != null) {
                try {
                    // Use UTF-8 encoding to read properties file
                    return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            
            return null;
        }
    };
    
    private CodeReadingNoteBundle() {
    }
    
    /**
     * Get the resource bundle with custom locale support
     */
    @NotNull
    private static ResourceBundle getBundle() {
        // Always get fresh locale from settings to support language switching
        Locale locale = LanguageSettings.getInstance().getEffectiveLocale();
        return ResourceBundle.getBundle(BUNDLE, locale, CodeReadingNoteBundle.class.getClassLoader(), UTF8_CONTROL);
    }
    
    /**
     * Get a message from the bundle
     * @param key the message key
     * @param params optional parameters for message formatting
     * @return the localized message
     */
    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        try {
            ResourceBundle bundle = getBundle();
            String value = bundle.getString(key);
            
            if (params.length > 0) {
                return MessageFormat.format(value, params);
            }
            return value;
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }
    
    /**
     * Get a message supplier for lazy evaluation
     * @param key the message key
     * @param params optional parameters for message formatting
     * @return supplier that provides the localized message
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return () -> message(key, params);
    }
}

