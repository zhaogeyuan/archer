package com.atpex.archer.expression;

/**
 * Cache util
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheExpressionUtilObject {

    public static String concat(String delimiter, Object... values) {
        StringBuilder stringBuilder = new StringBuilder();
        if (values.length > 0) {
            stringBuilder.append(values[0]);
        }
        for (int i = 1; i < values.length; i++) {
            stringBuilder.append(delimiter).append(values[i]);
        }
        return stringBuilder.toString();
    }

    public static String str(Object value) {
        return String.valueOf(value);
    }
}
