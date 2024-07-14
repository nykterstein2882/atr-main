package com.atr.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandleStick {
    private long time;
    private double open;
    private double close;
    private double high;
    private double low;
    private double volume;
}
