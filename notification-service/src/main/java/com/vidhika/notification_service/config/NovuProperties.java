package com.vidhika.notification_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "novu")
public class NovuProperties {

    /**
     * Secret API Key obtained from Novu dashboard or self-hosted instance.
     */
    private String apiKey = "dummy_api_key";

    /**
     * Base URL for the Novu API.
     * Default for Novu Cloud: https://api.novu.co/v1
     * Default for self-hosted: http://localhost:3000/v1
     */
    private String baseUrl = "https://api.novu.co/v1";

    /**
     * Flag to enable or disable Novu notification triggers.
     * Useful for toggling during local dev / test without hitting Novu.
     */
    private boolean enabled = false;
}