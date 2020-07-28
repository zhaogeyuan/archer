package com.atpex.archer.spring.autoconfigure;

import com.atpex.archer.spring.autoconfigure.annotation.EnableArcher;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Active mode config selector
 *
 * @author atpexgo.wu
 * @see EnableArcher
 * @since 1.0
 */
public class ActiveModeConfigSelector extends AdviceModeImportSelector<EnableArcher> {

    @Override
    public String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY:
                return getProxyImports();
            case ASPECTJ:
                return getAspectJImports();
            default:
                return null;
        }
    }

    /**
     * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#PROXY}.
     * <p>Take care of adding the necessary JSR-107 import if it is available.
     */
    private String[] getProxyImports() {
        List<String> result = new ArrayList<>();
        result.add(AutoProxyRegistrar.class.getName());
        result.add(ArcherProxyConfiguration.class.getName());
        return result.toArray(new String[0]);
    }

    /**
     * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#ASPECTJ}.
     * <p>Take care of adding the necessary JSR-107 import if it is available.
     */
    private String[] getAspectJImports() {
//        List<String> result = new ArrayList<>();
//        result.add(CACHE_ASPECT_CONFIGURATION_CLASS_NAME);
//        return result.toArray(new String[0]);
        throw new UnsupportedOperationException("AspectJ annotation is unsupported");
    }
}
