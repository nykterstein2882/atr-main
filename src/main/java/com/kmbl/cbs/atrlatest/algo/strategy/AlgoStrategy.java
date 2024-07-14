package com.kmbl.cbs.atrlatest.algo.strategy;

import com.kmbl.cbs.atrlatest.models.AlgoConfig;
import com.kmbl.cbs.atrlatest.models.Result;
import com.kmbl.cbs.atrlatest.models.Strategy;

public interface AlgoStrategy {
    Result getResult(AlgoConfig algoConfig);
    Strategy getStrategyName();
}
