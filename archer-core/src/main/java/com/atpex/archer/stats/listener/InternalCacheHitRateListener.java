package com.atpex.archer.stats.listener;

import com.atpex.archer.CacheManager;
import com.atpex.archer.stats.event.api.CacheEvent;
import com.atpex.archer.util.InfoPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Internal cache hit rate listener
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class InternalCacheHitRateListener implements CacheHitRateListener {

    private static final Logger logger = LoggerFactory.getLogger(InternalCacheHitRateListener.class);

    public static final String INTERNAL_METRICS_LISTENER_BEAN_NAME = "service.cacheable.internalLocalCacheMetricsListener";


    @Override
    public void onEvent(CacheHitRateEvent event) {
        // do something
        if (CacheManager.Config.metricsEnabled) {
            resolveCacheHitMiss(event);
        }
    }

    @Override
    public boolean filter(Class<? extends CacheEvent> eventClass) {
        return eventClass == CacheHitRateEvent.class;
    }


    public void startPrint() {
        Executors.newSingleThreadExecutor().submit(() -> {
            for (; ; ) {
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException ignored) {
                }
                doPrint();
            }
        });
    }

    private void doPrint() {
        if (hitRateInfo.size() > 0) {
            InfoPrinter.printHitRate(hitRateInfo);
        }
    }

    private Map<String, HitRateInfo> hitRateInfo = new ConcurrentHashMap<>();

    private void resolveCacheHitMiss(CacheHitRateEvent event) {
        // merge
        hitRateInfo.compute(event.getType(), (s, hitRateInfo) -> {
            if (hitRateInfo == null) {
                HitRateInfo newRateInfo = new HitRateInfo();
                newRateInfo.hitDataSize = event.getHitDataSize();
                newRateInfo.queryingTimes = event.getQueryingTimes();
                newRateInfo.totalDataSize = event.getTotalDataSize();
                newRateInfo.penetrated = event.isBreakdown();
                return newRateInfo;
            }
            hitRateInfo.hitDataSize += event.getHitDataSize();
            hitRateInfo.queryingTimes += event.getQueryingTimes();
            hitRateInfo.totalDataSize += event.getTotalDataSize();
            if (event.isBreakdown()) {
                hitRateInfo.penetrated = true;
            }
            return hitRateInfo;
        });
    }


    @Override
    public int compareTo(CacheMetricsListener<CacheHitRateEvent> o) {
        return 0;
    }

    public static class HitRateInfo {

        private int totalDataSize;

        private int hitDataSize;

        private int queryingTimes;

        private boolean penetrated;

        public int getTotalDataSize() {
            return totalDataSize;
        }

        public int getHitDataSize() {
            return hitDataSize;
        }

        public int getQueryingTimes() {
            return queryingTimes;
        }

        public boolean isPenetrated() {
            return penetrated;
        }
    }
}
