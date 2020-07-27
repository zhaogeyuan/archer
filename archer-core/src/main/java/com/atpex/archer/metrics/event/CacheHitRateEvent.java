package com.atpex.archer.metrics.event;

/**
 * Cache hit rate event
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class CacheHitRateEvent implements CacheEvent {

    /**
     * Cache type
     * <p>
     * not stable, may be optimized according to version
     */
    private String type;

    /**
     * Total requested data size, and it's always the same with parameters size
     * example:
     * User[] getUsers(int[] ids)
     * Assert ids.length is set to 10, then {@link #totalDataSize} will be set to 10 as well no matter if
     * the result User[].length is 10 or not.
     */
    private int totalDataSize;

    /**
     * Total requested data size which is loaded from cache implementation
     */
    private int hitDataSize;

    /**
     * Total times trying to query cache implementation
     */
    private int queryingTimes;

    /**
     * If cache is breakdown and the method is invoked
     */
    private boolean breakdown;


    private CacheHitRateEvent() {
    }

    public String getType() {
        return type;
    }

    public int getTotalDataSize() {
        return totalDataSize;
    }

    public int getHitDataSize() {
        return hitDataSize;
    }


    public int getQueryingTimes() {
        return queryingTimes;
    }


    public boolean isBreakdown() {
        return breakdown;
    }


    public static CacheHitRateEventBuilder eventBuilder(String type) {
        return new CacheHitRateEventBuilder(type);
    }

    public static class CacheHitRateEventBuilder {

        private String type;

        private int totalDataSize;

        private int hitDataSize;

        private int queryingTimes;

        private boolean breakdown;

        public CacheHitRateEventBuilder(String type) {
            this.type = type;
        }

        public CacheHitRateEventBuilder setTotalDataSize(int totalDataSize) {
            this.totalDataSize = totalDataSize;
            return this;
        }

        public CacheHitRateEventBuilder increaseTotalDataSize() {
            this.totalDataSize++;
            return this;
        }

        public CacheHitRateEventBuilder increaseTotalDataSizeBy(int extraDataSize) {
            this.totalDataSize += extraDataSize;
            return this;
        }

        public CacheHitRateEventBuilder setHitDataSize(int hitDataSize) {
            this.hitDataSize = hitDataSize;
            return this;
        }

        public CacheHitRateEventBuilder increaseHitDataSize() {
            this.hitDataSize++;
            return this;
        }

        public CacheHitRateEventBuilder increaseHitDataSizeBy(int hitDataSize) {
            this.hitDataSize += hitDataSize;
            return this;
        }

        public CacheHitRateEventBuilder setQueryingTimes(int queryingTimes) {
            this.queryingTimes = queryingTimes;
            return this;
        }

        public CacheHitRateEventBuilder increaseQueryingTimes() {
            this.queryingTimes++;
            return this;
        }

        public CacheHitRateEventBuilder increaseQueryingTimesBy(int queryingTimes) {
            this.queryingTimes += queryingTimes;
            return this;
        }

        public CacheHitRateEventBuilder setBreakdown(boolean breakdown) {
            this.breakdown = breakdown;
            return this;
        }

        public CacheHitRateEvent build() {
            CacheHitRateEvent cacheHitRateEvent = new CacheHitRateEvent();
            cacheHitRateEvent.type = type;
            cacheHitRateEvent.totalDataSize = totalDataSize;
            cacheHitRateEvent.hitDataSize = hitDataSize;
            cacheHitRateEvent.queryingTimes = queryingTimes;
            cacheHitRateEvent.breakdown = breakdown;
            return cacheHitRateEvent;
        }
    }

    @Override
    public String toString() {
        return "CacheHitRateEvent{" +
                "type='" + type + '\'' +
                ", totalDataSize=" + totalDataSize +
                ", hitDataSize=" + hitDataSize +
                ", queryingTimes=" + queryingTimes +
                ", breakdown=" + breakdown +
                '}';
    }
}
