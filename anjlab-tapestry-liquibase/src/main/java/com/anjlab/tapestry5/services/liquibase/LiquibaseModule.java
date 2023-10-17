/**
 * Copyright 2014 AnjLab
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.tapestry5.services.liquibase;

import java.util.Map;

import org.apache.tapestry5.commons.MappedConfiguration;
import org.apache.tapestry5.commons.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.http.services.ApplicationInitializerFilter;

public class LiquibaseModule
{
    public static final String LIQUIBASE_VERSION = "liquibase.version";
    
    public static final String LIQUIBASE_VERSION_2_X = "2.x";
    public static final String LIQUIBASE_VERSION_1_9_X = "1.9.x";
    
    public static final String LIQUIBASE_CHANGELOG = "liquibase.changelog";
    public static final String LIQUIBASE_DATA_SOURCE = "liquibase.datasource";
    public static final String LIQUIBASE_HOST_EXCLUDES = "liquibase.host.excludes";
    public static final String LIQUIBASE_HOST_INCLUDES = "liquibase.host.includes";
    public static final String LIQUIBASE_FAIL_ON_ERROR = "liquibase.onerror.fail";
    public static final String LIQUIBASE_CONTEXTS = "liquibase.contexts";
    
    public static final String LIQUIBASE_SHOULD_RUN = "liquibase.should.run";
    
    public static void bind(ServiceBinder binder)
    {
        binder.bind(LiquibaseInitializer.class);
    }

    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        configuration.add(LIQUIBASE_VERSION, LIQUIBASE_VERSION_2_X);
        
        configuration.add(LIQUIBASE_FAIL_ON_ERROR, "true");
        configuration.add(LIQUIBASE_SHOULD_RUN, "true");
        
        //  Empty values won't be set as init-parameters of ServletContext
        //  Setting them to empty strings here required
        //  so that SymbolSource#valueForSymbol() doesn't throw exceptions
        //  See LiquibaseInitializerImpl#addInitParameter()
        
        configuration.add(LIQUIBASE_CHANGELOG, "");
        configuration.add(LIQUIBASE_DATA_SOURCE, "");
        configuration.add(LIQUIBASE_HOST_EXCLUDES, "");
        configuration.add(LIQUIBASE_HOST_INCLUDES, "");
        configuration.add(LIQUIBASE_CONTEXTS, "");
    }
    
    public void contributeApplicationInitializer(
            OrderedConfiguration<ApplicationInitializerFilter> configuration,
            @Inject @Symbol(LIQUIBASE_SHOULD_RUN) boolean shouldRunLiquibase,
            LiquibaseInitializer liquibaseInitializer)
    {
        if (shouldRunLiquibase)
        {
            configuration.add("Liquibase", liquibaseInitializer);
        }
    }

    public void contributeLiquibaseInitializer(
            OrderedConfiguration<LiquibaseConfigurer> configuration)
    {
        configuration.add("default", new LiquibaseConfigurer()
        {
            @Override
            public String getConfigurationName()
            {
                return "default";
            }

            @Override
            public void configure(Map<String, String> configuration)
            {
                // NOOP
            }
        }, "before:*");
    }
}
