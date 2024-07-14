package com.kmbl.cbs.atrlatest.algo.strategy.impl;

import com.kmbl.cbs.atrlatest.algo.strategy.AlgoStrategy;
import com.kmbl.cbs.atrlatest.models.*;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.kmbl.cbs.atrlatest.util.Utils.*;

@NoArgsConstructor
public class ExecuteFirstOnly implements AlgoStrategy {
    @SneakyThrows
    @Override
    public Result getResult(AlgoConfig algoConfig) {
        List<CandleStick> dailyCandles = loadDailyCandlesFromJson(algoConfig);
        List<CandleStick> hourlyCandles = loadHourlyCandlesFromJson(algoConfig);

        Map<String, CandleStick> dailyMap = getDailyMap(dailyCandles);
        Map<String, List<CandleStick>> hourlyMap = getHourlyMap(hourlyCandles);

        List<CandleStick> candlesForAnalysis = getCandlesForAnalysis(dailyCandles, algoConfig);
        return getResult(candlesForAnalysis, dailyMap, hourlyMap, algoConfig);
    }

    @Override
    public Strategy getStrategyName() {
        return Strategy.EXECUTE_FIRST_STRATEGY;
    }

    private List<CandleStick> getCandlesForAnalysis(List<CandleStick> dailyCandles, AlgoConfig algoConfig) {
        return dailyCandles.subList(Math.max(0, dailyCandles.size() - algoConfig.getLastDays()), dailyCandles.size());
    }

    private Map<String, List<CandleStick>> getHourlyMap(List<CandleStick> hourlyCandles) {
        Map<String, List<CandleStick>> hourlyMap = new HashMap<>();
        hourlyCandles.forEach(candleStick -> {
            String day = getDayFromTimestamp(candleStick.getTime());
            if(!hourlyMap.containsKey(day)) {
                hourlyMap.put(day, new ArrayList<>(){{add(candleStick);}});
            } else {
                hourlyMap.get(day).add(candleStick);
            }
        });
        return hourlyMap;
    }

    private Map<String, CandleStick> getDailyMap(List<CandleStick> dailyCandles) {
        return dailyCandles.stream()
                .collect(Collectors.toMap(
                        candleStick -> getDayFromTimestamp(candleStick.getTime()),
                        candleStick -> candleStick
                ));
    }

    private String getDayFromTimestamp(long unixTimestamp) {
        Date date = new Date(unixTimestamp * 1000L - 2 * 60 * 60 * 1000L);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy");
        return simpleDateFormat.format(date);
    }

    private Result getResult(List<CandleStick> dailyCandles, Map<String, CandleStick> dailyMap,
                             Map<String, List<CandleStick>> hourlyMap, AlgoConfig algoConfig) {
        Result result = new Result();
        result.setAlgoConfig(algoConfig);
        result.setDays(dailyCandles.size() - 1);
        result.setStrategy(this.getStrategyName());

        for ( int i=0; i < dailyCandles.size() - 1 ; i++) {
            CandleStick previousDayCandle = dailyCandles.get(i);
            CandleStick currentDayCandle = dailyCandles.get(i + 1);
            Result.CandleStickResult candleStickResult = getCandleStickResult(previousDayCandle,
                    hourlyMap.get(getDayFromTimestamp(currentDayCandle.getTime())), algoConfig);

            if (TradeStatus.NO_TRADE.equals(candleStickResult.getTradeStatus())) {
                System.out.println(TradeStatus.NO_TRADE + " for " + getDayFromTimestamp(dailyCandles.get(i+1).getTime()));
                result.getNoTrades().add(candleStickResult);
            } else if (candleStickResult.getProfit() > 0) {
                System.out.println(TradeStatus.PROFIT + " for " + getDayFromTimestamp(dailyCandles.get(i+1).getTime()));
                result.getProfitDays().add(candleStickResult);
            } else if (candleStickResult.getProfit() < 0) {
                System.out.println(TradeStatus.LOSS + " for " + getDayFromTimestamp(dailyCandles.get(i+1).getTime()));
                result.getNonProfitDays().add(candleStickResult);
            }

            result.setProfit(result.getProfit() + candleStickResult.getProfit());
            result.setMaxProfit(result.getMaxProfit() + candleStickResult.getMaxProfit());
            result.setNumNonProfitDays(result.getNonProfitDays().size());
            result.setNumProfitDays(result.getProfitDays().size());
            result.setNumNoTrades(result.getNoTrades().size());
        }

        return result;
    }

    private Result.CandleStickResult getCandleStickResult(CandleStick previousDayCandle,
                                                          List<CandleStick> todaysHourlyCandles,
                                                          AlgoConfig algoConfig) {
        Result.CandleStickResult longResult = checkLongTrade(previousDayCandle, todaysHourlyCandles, algoConfig);
        Result.CandleStickResult shortResult = checkShortTrade(previousDayCandle, todaysHourlyCandles, algoConfig);
        Result.CandleStickResult candleStickResult;

        if(TradeStatus.NO_TRADE.equals(longResult.getTradeStatus()) && TradeStatus.NO_TRADE.equals(shortResult.getTradeStatus())) {
            candleStickResult = new Result.CandleStickResult();
        } else if (TradeStatus.NO_TRADE.equals(longResult.getTradeStatus())) {
            //Meaning only shortTrade was made on that day
            candleStickResult = shortResult;
        } else if (TradeStatus.NO_TRADE.equals(shortResult.getTradeStatus())) {
            //Meaning only longTrade was made on that day
            candleStickResult = longResult;
        } else {
            candleStickResult = (longResult.getEntryIndex() < shortResult.getEntryIndex()) ? longResult : shortResult;
        }

        candleStickResult.setDay(getDayFromTimestamp(todaysHourlyCandles.get(0).getTime()));
        candleStickResult.setSuperFields(previousDayCandle);
        return candleStickResult;
    }

    private Result.CandleStickResult checkShortTrade(CandleStick previousDayCandle,
                                                     List<CandleStick> todaysHourlyCandles,
                                                     AlgoConfig algoConfig) {
        if(todaysHourlyCandles.size() <= 3) {
            return new Result.CandleStickResult();
        }
        Result.CandleStickResult shortResult = new Result.CandleStickResult();
        boolean crossedHigh = false;
        boolean crossedHighAndClosedLow = false;
        boolean tradeExecuted = false;
        boolean tradeFinished = false;
        TradeStatus tradeStatus = TradeStatus.NO_TRADE;
        boolean stopLossHit = false;
        double highSoFar = 0;
        double entry = 0;
        double profit = 0;
        double stopLoss = 0;

        for (int i = 0; i < todaysHourlyCandles.size(); i++) {
            if (tradeExecuted) {
                if(!tradeFinished) {
                    shortResult.setMaxProfit(Math.max(shortResult.getMaxProfit(), getCurrentShortTradeMaxProfits(entry,
                            stopLoss, todaysHourlyCandles.get(i), algoConfig)));
                    if(todaysHourlyCandles.get(i).getHigh() >= stopLoss) {
                        System.out.println("stoploss hit");
                        stopLossHit = true;
                        break;
                    }
                } else {
                    //as we're no longer in the trade
                    break;
                }
            } else {
                highSoFar = Math.max(highSoFar, todaysHourlyCandles.get(i).getHigh());

                if (todaysHourlyCandles.get(i).getHigh() > previousDayCandle.getHigh()) {
                    crossedHigh = true;
                }
                if ( crossedHigh && todaysHourlyCandles.get(i).getClose() < previousDayCandle.getHigh()) {
                    crossedHighAndClosedLow = true;
                }

                if(crossedHighAndClosedLow) {
                    tradeExecuted = true;
                    entry = todaysHourlyCandles.get(i).getClose();
                    stopLoss = highSoFar + algoConfig.getSpread();
                    shortResult.setEntry(todaysHourlyCandles.get(i).getClose());
                    shortResult.setStopLoss(highSoFar + algoConfig.getSpread());
                    shortResult.setEntryIndex(i);
                }
            }
        }

        if (!tradeExecuted) {
            tradeStatus = TradeStatus.NO_TRADE;
        } else if(stopLossHit) {
            System.out.println(String.format("Stoploss hit but maxProfit was: %s", shortResult.getMaxProfit()));
            shortResult.setProfit((
                    shortResult.getMaxProfit() >= algoConfig.getThresholdProfit())
                    ? algoConfig.getThresholdProfit()
                    : -1
            );
        } else if (tradeExecuted) {
            shortResult.setProfit((shortResult.getMaxProfit() >= algoConfig.getThresholdProfit())
                    ? algoConfig.getThresholdProfit()
                    : Math.min(algoConfig.getThresholdProfit(), getCurrentShortTradeProfits(entry, stopLoss,
                        todaysHourlyCandles.get(todaysHourlyCandles.size() - 1), algoConfig))
            );
        }

        if(shortResult.getProfit() != 0) {
            shortResult.setTradeStatus(shortResult.getProfit() > 0 ? TradeStatus.PROFIT : TradeStatus.LOSS);
        }

        shortResult.setStopLoss(stopLoss);
        shortResult.setTime(previousDayCandle.getTime());

        return shortResult;
    }

    private Result.CandleStickResult checkLongTrade(CandleStick previousDayCandle,
                                                    List<CandleStick> todaysHourlyCandles,
                                                    AlgoConfig algoConfig) {
        if(todaysHourlyCandles.size() <= 3) {
            return new Result.CandleStickResult();
        }
        Result.CandleStickResult longResult = new Result.CandleStickResult();
        boolean crossedLow = false;
        boolean crossedLowAndClosedUp = false;
        boolean tradeExecuted = false;
        boolean tradeFinished = false;
        TradeStatus tradeStatus = TradeStatus.NO_TRADE;
        boolean stopLossHit = false;
        double lowSoFar = 100000000D;
        double entry = 0;
        double profit = 0;
        double stopLoss = 0;

        for (int i = 0; i < todaysHourlyCandles.size(); i++) {
            if (tradeExecuted) {
                if(!tradeFinished) {
                    longResult.setMaxProfit(Math.max(longResult.getMaxProfit(), getCurrentLongTradeMaxProfits(entry,
                            stopLoss, todaysHourlyCandles.get(i), algoConfig)));
                    if(todaysHourlyCandles.get(i).getLow() <= stopLoss) {
                        stopLossHit = true;
                        System.out.println("Stoploss hit");
                        break;
                    }
                } else {
                    //as we're no longer in the trade
                    break;
                }
            } else {
                lowSoFar = Math.min(lowSoFar, todaysHourlyCandles.get(i).getLow());

                if (todaysHourlyCandles.get(i).getLow() < previousDayCandle.getLow()) {
                    crossedLow = true;
                }
                if ( crossedLow && todaysHourlyCandles.get(i).getClose() > previousDayCandle.getLow()) {
                    crossedLowAndClosedUp = true;
                }

                if(crossedLowAndClosedUp) {
                    tradeExecuted = true;
                    entry = todaysHourlyCandles.get(i).getClose();
                    stopLoss = lowSoFar - algoConfig.getSpread();
                    longResult.setEntry(todaysHourlyCandles.get(i).getClose());
                    longResult.setStopLoss(lowSoFar - algoConfig.getSpread());
                    longResult.setEntryIndex(i);
                }
            }
        }

        if (!tradeExecuted) {
            tradeStatus = TradeStatus.NO_TRADE;
        } else if(stopLossHit) {
            System.out.println(String.format("Stoploss hit but maxProfit was: %s", longResult.getMaxProfit()));
            longResult.setProfit((longResult.getMaxProfit() >= algoConfig.getThresholdProfit())
                    ? algoConfig.getThresholdProfit()
                    : -1
            );
        } else {
            longResult.setProfit(
                    (longResult.getMaxProfit() >= algoConfig.getThresholdProfit())
                    ? algoConfig.getThresholdProfit()
                    : Math.min(algoConfig.getThresholdProfit(), getCurrentLongTradeProfits(entry, stopLoss,
                        todaysHourlyCandles.get(todaysHourlyCandles.size() - 1), algoConfig))
            );
        }

        if (longResult.getProfit() != 0) {
            longResult.setTradeStatus(longResult.getProfit() > 0 ? TradeStatus.PROFIT : TradeStatus.LOSS);
        }

        longResult.setStopLoss(stopLoss);
        longResult.setTime(previousDayCandle.getTime());

        return longResult;
    }
}
