package com.nitin.saas.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads key=value pairs from a .env file in the working directory into the
 * Spring Environment <em>before</em> any @Value / @ConfigurationProperties
 * processing occurs.
 *
 * Rules:
 *   - Lines starting with # are comments and are ignored.
 *   - Blank lines are ignored.
 *   - Values may optionally be wrapped in single or double quotes.
 *   - Environment variables already set in the OS take precedence because
 *     this source is added at the END of the property-source chain.
 */
public class DotEnvInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = Logger.getLogger(DotEnvInitializer.class.getName());
    private static final String ENV_FILE = ".env";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        File envFile = new File(ENV_FILE);
        if (!envFile.exists() || !envFile.isFile()) {
            log.fine(".env file not found – skipping DotEnvInitializer");
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key   = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                // Strip surrounding quotes
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                props.put(key, value);
            }
        } catch (Exception ex) {
            log.warning("Failed to read .env file: " + ex.getMessage());
            return;
        }

        if (!props.isEmpty()) {
            ConfigurableEnvironment env = applicationContext.getEnvironment();
            // addLast so that OS environment variables and -D flags win
            env.getPropertySources().addLast(new MapPropertySource("dotEnvProperties", props));
            log.info("DotEnvInitializer: loaded " + props.size() + " properties from " + ENV_FILE);
        }
    }
}