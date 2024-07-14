package com.atr.algo.strategy;

import com.atr.models.AlgoConfig;
import com.atr.models.Result;
import com.atr.models.Strategy;

public interface AlgoStrategy {
    Result getResult(AlgoConfig algoConfig);
    Strategy getStrategyName();
}
