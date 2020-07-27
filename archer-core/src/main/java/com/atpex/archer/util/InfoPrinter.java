package com.atpex.archer.util;

import com.atpex.archer.CacheManager;
import com.atpex.archer.components.KeyGenerator;
import com.atpex.archer.components.Serializer;
import com.atpex.archer.metrics.listener.InternalCacheHitRateListener;
import com.atpex.archer.operation.impl.CacheOperation;
import com.atpex.archer.operation.impl.EvictionOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Info printer
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class InfoPrinter {

    private static final Logger logger = LoggerFactory.getLogger(InfoPrinter.class);

    /**
     * Print component usage info
     * <p>
     * for debugging
     *
     * @param cacheManager
     */
    public static void printComponentUsageInfo(CacheManager cacheManager) {
        Map<String, EvictionOperation> evictionConfigMap = cacheManager.getEvictionOperationMap();
        Map<String, CacheOperation> acceptationConfigMap = cacheManager.getCacheOperationMap();
        Map<String, KeyGenerator> keyGeneratorMap = cacheManager.getKeyGeneratorMap();
        Map<String, Serializer> serializerMap = cacheManager.getSerializerMap();
        Map<String, List<String>> methodSignatureToOperationSourceName = cacheManager.getMethodSignatureToOperationSourceName();

        String evictionConfigInfo = generateTableWithLastColumnBorderless("eviction operation sources", Collections.singletonList("bean"), evictionConfigMap.keySet().stream().map(Collections::singletonList).collect(Collectors.toList()), 50);
        String acceptationConfigInfo = generateTableWithLastColumnBorderless("acceptation operation sources", Collections.singletonList("bean"), acceptationConfigMap.keySet().stream().map(Collections::singletonList).collect(Collectors.toList()), 50);
        String keyGeneratorInfo = generateTableWithLastColumnBorderless("key generators", Collections.singletonList("bean"), keyGeneratorMap.keySet().stream().map(Collections::singletonList).collect(Collectors.toList()), 50);
        String serializerInfo = generateTableWithLastColumnBorderless("serializers", Collections.singletonList("bean"), serializerMap.keySet().stream().map(Collections::singletonList).collect(Collectors.toList()), 50);
        String methodsInfo = generateTableWithLastColumnBorderless("proxied methods", Collections.singletonList("methods"), methodSignatureToOperationSourceName.keySet().stream().map(Collections::singletonList).collect(Collectors.toList()), 50);

        String configInfo = generateTableWithLastColumnBorderless("config", Arrays.asList("key", "value"), Arrays.asList(
                Arrays.asList("serialization", CacheManager.Config.valueSerialization.name()),
                Arrays.asList("metricsEnabled", String.valueOf(CacheManager.Config.metricsEnabled))
        ), 30);

        logger.debug(configInfo + evictionConfigInfo + acceptationConfigInfo + keyGeneratorInfo + serializerInfo + methodsInfo);
    }

    private static String generateTableWithLastColumnBorderless(String header, List<String> titles, List<List<String>> values, int width) {
        StringBuilder stringBuilder = new StringBuilder(System.lineSeparator());
        stringBuilder.append(System.lineSeparator()).append(header.toUpperCase()).append(generateTitle(width, titles.toArray(new String[0])));
        for (List<String> value : values) {
            stringBuilder.append(generateValueBorderless(width, value.toArray()));
        }
        stringBuilder.append(generateFooter(width, titles.size()));
        return stringBuilder.toString();
    }

    /**
     * @param hitRateInfo
     */
    public static void printHitRate(Map<String, InternalCacheHitRateListener.HitRateInfo> hitRateInfo) {
        StringBuilder stringBuilder =
                new StringBuilder(generateTitle(12,
                        "Hit",
                        "Total",
                        "Rate",
                        "Penetrated",
                        "Q-times",
                        "Type"));
        hitRateInfo.forEach((t, info) -> stringBuilder.append(generateValueBorderless(12,
                info.getHitDataSize(),
                info.getTotalDataSize(),
                percentage(info.getHitDataSize(), info.getTotalDataSize()),
                info.isPenetrated(),
                info.getQueryingTimes(),
                t)));
        stringBuilder.append(generateFooter(12, 6));
        logger.debug(stringBuilder.toString());
    }


    public static String generateTitle(int width, String... titles) {
        StringBuilder stringBuilder = new StringBuilder(System.lineSeparator());
        for (String title : titles) {
            stringBuilder.append("+");
            if (width > title.length()) {
                int total = 0;
                int side = (width - title.length()) / 2;
                for (int i = 0; i < side; i++) {
                    total++;
                    stringBuilder.append("-");
                }
                total += title.length();
                stringBuilder.append(title);
                for (int i = 0; i < side; i++) {
                    total++;
                    if (total > width) {
                        break;
                    }
                    stringBuilder.append("-");
                }
                if (total < width) {
                    stringBuilder.append("-");
                }
            } else {
                stringBuilder.append("-").append(title).append("-");
            }
        }
        stringBuilder.append("+");
        return stringBuilder.toString();
    }

    public static String generateValue(int width, Object... values) {
        StringBuilder stringBuilder = new StringBuilder(System.lineSeparator());
        for (Object value : values) {
            stringBuilder.append("|").append(paddingRight(value, width));
        }
        stringBuilder.append("|");
        return stringBuilder.toString();
    }

    public static String generateValueBorderless(int width, Object... values) {
        StringBuilder stringBuilder = new StringBuilder(System.lineSeparator());
        for (int i = 0; i < values.length - 1; i++) {
            stringBuilder.append("|").append(paddingRight(values[i], width));
        }

        stringBuilder.append("|").append(values[values.length - 1]);
        return stringBuilder.toString();
    }

    public static String generateFooter(int width, int count) {
        StringBuilder stringBuilder = new StringBuilder(System.lineSeparator());
        for (int i = 0; i < count; i++) {
            stringBuilder.append("+");
            for (int j = 0; j < width; j++) {
                stringBuilder.append("-");
            }
        }
        stringBuilder.append("+");
        return stringBuilder.toString();
    }

    private static String paddingRight(Object oriO, int paddingTo) {
        String ori = String.valueOf(oriO);
        int oriLength;
        if ((oriLength = ori.length()) < paddingTo) {
            StringBuilder stringBuilder = new StringBuilder(ori);
            while (oriLength < paddingTo) {
                stringBuilder.append(" ");
                oriLength++;
            }
            return stringBuilder.toString();
        } else if (ori.length() > paddingTo) {
            return ori.substring(0, paddingTo - 3) + "...";
        }
        return ori;
    }

    private static String percentage(long hit, long total) {
        if (hit == 0) {
            return "0%";
        }
        BigDecimal result = BigDecimal.valueOf(hit).divide(BigDecimal.valueOf(total), 2, RoundingMode.CEILING).multiply(BigDecimal.valueOf(100));
        return result.toString() + "%";
    }

}
