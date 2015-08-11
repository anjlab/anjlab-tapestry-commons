package com.anjlab.tapestry5.services;

import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.services.SymbolProvider;

public class CommonsModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(InjectionHelper.class);
        binder.bind(ConfigHelper.class);
    }

    public static void contributeSymbolSource(
            OrderedConfiguration<SymbolProvider> configuration,
            ObjectLocator objectLocator)
    {
        final ConfigHelper configHelper = objectLocator.getService(ConfigHelper.class);

        configuration.add("ConfigHelper", new SymbolProvider()
        {
            @Override
            public String valueForSymbol(String symbolName)
            {
                return configHelper.get(symbolName);
            }
        }, "after:SystemProperties", "before:ApplicationDefaults");
    }
}