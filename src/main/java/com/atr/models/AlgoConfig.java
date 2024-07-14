package com.atr.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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

    private static Map<String, Double> TokenToSpreadMap;

    static {
        TokenToSpreadMap = new HashMap<>() {{
            put("GBPUSD", 0.00012);
            put("GBPJPY", 0.097);
            put("EURJPY", 0.085);
            put("AUDUSD", 0.00014);
            put("AUDCAD", 0.00025);
            put("USDJPY", 0.08);
            put("Gold", 0.2);
            put("silver", 0.04);
        }};
    }

    public void setToken(String token) {
        this.token = token;
        setSpreadByToken(token);
    }

    public void setSpreadByToken(String token) {
        if(TokenToSpreadMap.containsKey(token)) {
            System.out.println("Token: " + token + " Spread: " + TokenToSpreadMap.get(token));
            this.spread = TokenToSpreadMap.get(token);
        } else {
            System.out.println("Token " + token + " not found");
            this.spread = 0;
        }
    }

}
