package com.atr.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
    private AlgoConfig algoConfig;
    private Strategy strategy;
    private double profit;
    private double maxProfit;
    private int days;
    private int numProfitDays;
    private int numNonProfitDays;
    private int numNoTrades;
    private int numIgnoredTrades;
    List<CandleStickResult> profitDays = new ArrayList<>();
    List<CandleStickResult> nonProfitDays = new ArrayList<>();
    List<CandleStickResult> noTrades = new ArrayList<>();
    List<CandleStickResult> ignoredTrades = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CandleStickResult extends CandleStick {
        private double profit = 0;
        private double maxProfit = 0;
        private double entry;
        private double exit;
        private double stopLoss;
        private TradeStatus tradeStatus = TradeStatus.NO_TRADE;
        private String remark;
        private int entryIndex = 40;
        private String day;
        
        public void setSuperFields(CandleStick candleStick) {
            this.setHigh(candleStick.getHigh());
            this.setLow(candleStick.getLow());
            this.setOpen(candleStick.getOpen());
            this.setClose(candleStick.getClose());
        }
    }
}
