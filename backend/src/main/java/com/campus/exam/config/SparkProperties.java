package com.campus.exam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spark")
public record SparkProperties(String apiUrl, String apiPassword, String model) {
}
