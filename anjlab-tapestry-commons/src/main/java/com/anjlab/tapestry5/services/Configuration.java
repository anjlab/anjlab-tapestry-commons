package com.anjlab.tapestry5.services;

import org.apache.tapestry5.ioc.MappedConfiguration;

public interface Configuration
{
    String get(String propertyName);

    void add(String propertyName, MappedConfiguration<String, Object> configuration);

    void override(String propertyName, MappedConfiguration<String, Object> configuration);

    void overrideIfExists(String propertyName, MappedConfiguration<String, Object> configuration);

    void addIfExists(String propertyName, MappedConfiguration<String, Object> configuration);
}