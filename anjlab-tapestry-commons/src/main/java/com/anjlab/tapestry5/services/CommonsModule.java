package com.anjlab.tapestry5.services;

import org.apache.tapestry5.ioc.ServiceBinder;

public class CommonsModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(InjectionHelper.class);
        binder.bind(ConfigHelper.class);
    }
}