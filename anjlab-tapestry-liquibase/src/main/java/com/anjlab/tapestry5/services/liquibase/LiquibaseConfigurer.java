package com.anjlab.tapestry5.services.liquibase;

import java.util.Map;

public interface LiquibaseConfigurer
{
    String getConfigurationName();

    void configure(Map<String, String> configuration);
}