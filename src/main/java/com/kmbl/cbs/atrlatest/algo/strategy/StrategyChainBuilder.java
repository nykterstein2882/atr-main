package com.kmbl.cbs.atrlatest.algo.strategy;

import com.kmbl.cbs.atrlatest.models.AlgoConfig;
import com.kmbl.cbs.atrlatest.models.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class StrategyChainBuilder {
    private final List<AlgoStrategy> algoStrategies;

    public List<Result> executeAndGetResults(AlgoConfig algoConfig) {
        return algoStrategies.stream()
                .map(algoStrategy -> algoStrategy.getResult(algoConfig))
                .sorted(Comparator.comparingDouble(Result::getProfit))
                .collect(Collectors.toList());
    }
}
