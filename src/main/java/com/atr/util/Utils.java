package com.atr.util;

import com.atr.algo.strategy.impl.ExecuteFirstWithBounds;
import com.atr.models.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.atr.algo.strategy.AlgoStrategy;
import com.atr.algo.strategy.impl.DayCloseStrategy;
import com.atr.algo.strategy.impl.ExecuteAnyStrategy;
import com.atr.algo.strategy.impl.ExecuteFirstOnly;
import com.atr.models.AlgoConfig;
import com.atr.models.CandleStick;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class Utils {

    public static ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    public List<CandleStick> loadCandlesFromJson(String path) throws IOException {
        return objectMapper.readValue(new File(path), new TypeReference<>() {});
    }

    public String getHourlyCandleDataPath(AlgoConfig algoConfig) {
        return String.format("%s/%s/%s", algoConfig.getBasePath(), algoConfig.getToken(), algoConfig.getHourlySuffix());
    }

    public String getDailyCandleDataPath(AlgoConfig algoConfig) {
        return String.format("%s/%s/%s", algoConfig.getBasePath(), algoConfig.getToken(), algoConfig.getDailySuffix());
    }

    public List<CandleStick> loadHourlyCandlesFromJson(AlgoConfig algoConfig) throws IOException {
        return objectMapper.readValue(new File(getHourlyCandleDataPath(algoConfig)), new TypeReference<>() {});
    }

    public List<CandleStick> loadDailyCandlesFromJson(AlgoConfig algoConfig) throws IOException {
        return objectMapper.readValue(new File(getDailyCandleDataPath(algoConfig)), new TypeReference<>() {});
    }

    public String getResultPath(AlgoConfig algoConfig) {
        return String.format("%s/%s/%s", algoConfig.getBasePath(), algoConfig.getToken(), algoConfig.getResultSuffix());
    }

    public double getCurrentLongTradeProfits(double entry, double stopLoss, CandleStick currentCandleStick, AlgoConfig algoConfig) {
        double downUnits = entry - stopLoss;
        double currentUnits = currentCandleStick.getClose() - entry - algoConfig.getSpread();
        return currentUnits/downUnits;
    }

    public double getCurrentLongTradeMaxProfits(double entry, double stopLoss, CandleStick currentCandleStick, AlgoConfig algoConfig) {
        double downUnits = entry - stopLoss;
        double currentUnits = currentCandleStick.getHigh() - entry - algoConfig.getSpread();
        return currentUnits/downUnits;
    }

    public double getCurrentShortTradeProfits(double entry, double stopLoss, CandleStick currentCandleStick, AlgoConfig algoConfig) {
        double downUnits = stopLoss - entry;
        double currentUnits = entry - currentCandleStick.getHigh() - algoConfig.getSpread();
        return currentUnits/downUnits;
    }

    public double getCurrentShortTradeMaxProfits(double entry, double stopLoss, CandleStick currentCandleStick, AlgoConfig algoConfig) {
        double downUnits = stopLoss - entry;
        double currentUnits = entry - currentCandleStick.getLow() - algoConfig.getSpread();
        return currentUnits/downUnits;
    }

    public void writeResultToFile(List<Result> result, AlgoConfig algoConfig) {
        try {
            FileWriter file = new FileWriter(getResultPath(algoConfig));
            file.write(Utils.objectMapper.writeValueAsString(result));
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //note: add strategies here
    public List<AlgoStrategy> getAlgoStrategies() {
        return Arrays.asList(
                new DayCloseStrategy(),
                new ExecuteAnyStrategy(),
                new ExecuteFirstOnly(),
                new ExecuteFirstWithBounds()
        );
    }
}
