package com.github.attt.archer.cache.redis;


/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class Constant {

    public static final int DEFAULT_TIMEOUT = 2000;

    public static final int DEFAULT_MAX_TOTAL = 8;

    public static final int DEFAULT_MAX_IDLE = 8;

    public static final int DEFAULT_MIN_IDLE = 0;

    public static final boolean DEFAULT_LIFO = true;

    public static final boolean DEFAULT_FAIRNESS = false;

    public static final long DEFAULT_MAX_WAIT_MILLIS = -1L;

    public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS = 1000L * 60L * 30L;

    public static final int DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 3;

    public static final boolean DEFAULT_TEST_ON_CREATE = false;

    public static final boolean DEFAULT_TEST_ON_BORROW = false;

    public static final boolean DEFAULT_TEST_ON_RETURN = false;

    public static final boolean DEFAULT_TEST_WHILE_IDLE = false;

    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = -1L;

    public static final boolean DEFAULT_BLOCK_WHEN_EXHAUSTED = true;

}
