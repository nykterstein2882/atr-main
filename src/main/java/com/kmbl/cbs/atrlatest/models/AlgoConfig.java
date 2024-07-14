package com.kmbl.cbs.atrlatest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "algo-config")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgoConfig {
    private String basePath;
    private double spread;
    private double thresholdProfit;
    private int lastDays;
    private String hourlySuffix;
    private String dailySuffix;
    private String token;
    private String resultSuffix;
    private int lowerBound;
    private int upperBound;
}
