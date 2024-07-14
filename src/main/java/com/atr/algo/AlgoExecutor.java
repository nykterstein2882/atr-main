package com.atr.algo;

import com.atr.models.Result;
import com.atr.algo.strategy.StrategyChainBuilder;
import com.atr.util.Utils;
import com.atr.models.AlgoConfig;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class AlgoExecutor {

    @SneakyThrows
    public AlgoExecutor(AlgoConfig algoConfig) {
        StrategyChainBuilder strategyChainBuilder = new StrategyChainBuilder(Utils.getAlgoStrategies());
        List<Result> results = strategyChainBuilder.executeAndGetResults(algoConfig);
        analyseResults(results);
        Utils.writeResultToFile(results, algoConfig);
    }

    private void analyseResults(List<Result> results) {
        for (Result result : results) {
            System.out.println("\nAnalysing results keep quite!");
            System.out.println(result.getStrategy().toString());
            Map<Integer, List<Result.CandleStickResult>> indexDistribution = new HashMap<>();
            segregate(indexDistribution, result);
            for (Map.Entry<Integer, List<Result.CandleStickResult>> entry : indexDistribution.entrySet()) {
                List<Result.CandleStickResult> distributedValues = entry.getValue().stream()
                        .sorted(Comparator.comparingDouble(Result.CandleStickResult::getProfit))
                        .toList();
                System.out.println(entry.getKey() + ": " + distributedValues.stream().map(Result.CandleStickResult::getProfit).toList());
                System.out.println(entry.getKey() + ": " + distributedValues.stream().map(Result.CandleStickResult::getMaxProfit).toList());
            }
            System.out.println("Done");
        }
    }

    private void segregate(Map<Integer, List<Result.CandleStickResult>> indexDistribution, Result result) {
        segregateList(indexDistribution, result.getProfitDays());
        segregateList(indexDistribution, result.getNonProfitDays());
    }

    private void segregateList(Map<Integer, List<Result.CandleStickResult>> indexDistribution,
                               List<Result.CandleStickResult> dayResults) {
        for(Result.CandleStickResult candleStickResult: dayResults) {
            if (indexDistribution.containsKey(candleStickResult.getEntryIndex())) {
                indexDistribution.get(candleStickResult.getEntryIndex()).add(candleStickResult);
            } else {
                indexDistribution.put(candleStickResult.getEntryIndex(), new ArrayList<>(){{add(candleStickResult);}});
            }
        }
    }

}