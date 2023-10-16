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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.services.ApplicationGlobals;
import org.apache.tapestry5.services.ApplicationInitializer;
import org.apache.tapestry5.services.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquibaseInitializerImpl implements LiquibaseInitializer
{
    private static final Logger logger = LoggerFactory.getLogger(LiquibaseInitializerImpl.class);

    @Inject
    private ApplicationGlobals applicationGlobals;

    @Inject
    private SymbolSource symbolSource;

    @Inject
    @Symbol(LiquibaseModule.LIQUIBASE_VERSION)
    private String liquibaseVersion;

    private static final String LIQUIBASE_LISTENER_CLASS_NAME = "liquibase.listener.classname";

    private Map<String, List<LiquibaseConfigurer>> configurers;

    public LiquibaseInitializerImpl(List<LiquibaseConfigurer> configurers)
    {
        this.configurers = new HashMap<>();

        for (LiquibaseConfigurer configurer : configurers)
        {
            List<LiquibaseConfigurer> list =
                    this.configurers.get(configurer.getConfigurationName());

            if (list == null)
            {
                list = new ArrayList<>();
                this.configurers.put(configurer.getConfigurationName(), list);
            }

            list.add(configurer);
        }
    }

    private Map<String, String> getNames()
    {
        Map<String, String> names = new HashMap<String, String>();

        if (LiquibaseModule.LIQUIBASE_VERSION_2_X.equalsIgnoreCase(liquibaseVersion))
        {
            names.put(LiquibaseModule.LIQUIBASE_CHANGELOG, "liquibase.changelog");
            names.put(LiquibaseModule.LIQUIBASE_DATA_SOURCE, "liquibase.datasource");
            names.put(LiquibaseModule.LIQUIBASE_HOST_EXCLUDES, "liquibase.host.excludes");
            names.put(LiquibaseModule.LIQUIBASE_HOST_INCLUDES, "liquibase.host.includes");
            names.put(LiquibaseModule.LIQUIBASE_FAIL_ON_ERROR, "liquibase.onerror.fail");
            names.put(LiquibaseModule.LIQUIBASE_CONTEXTS,  "liquibase.contexts");

            names.put(LIQUIBASE_LISTENER_CLASS_NAME, "liquibase.integration.servlet.LiquibaseServletListener");
        }
        else if (LiquibaseModule.LIQUIBASE_VERSION_1_9_X.equalsIgnoreCase(liquibaseVersion))
        {
            names.put(LiquibaseModule.LIQUIBASE_CHANGELOG, "LIQUIBASE_CHANGELOG");
            names.put(LiquibaseModule.LIQUIBASE_DATA_SOURCE, "LIQUIBASE_DATA_SOURCE");
            names.put(LiquibaseModule.LIQUIBASE_HOST_EXCLUDES, "LIQUIBASE_HOST_EXCLUDES");
            names.put(LiquibaseModule.LIQUIBASE_HOST_INCLUDES, "LIQUIBASE_HOST_INCLUDES");
            names.put(LiquibaseModule.LIQUIBASE_FAIL_ON_ERROR, "LIQUIBASE_FAIL_ON_ERROR");
            names.put(LiquibaseModule.LIQUIBASE_CONTEXTS,  "LIQUIBASE_CONTEXTS");

            names.put(LIQUIBASE_LISTENER_CLASS_NAME, "liquibase.servlet.LiquibaseServletListener");
        }
        else
        {
            throw new RuntimeException(MessageFormat.format(
                    "Unsupported value of {0}={1}. Supported values are: {2} and {3}",
                    LiquibaseModule.LIQUIBASE_VERSION,
                    liquibaseVersion,
                    LiquibaseModule.LIQUIBASE_VERSION_1_9_X,
                    LiquibaseModule.LIQUIBASE_VERSION_2_X));
        }

        return names;
    }

    @Override
    public void initializeApplication(Context context, ApplicationInitializer initializer)
    {
        Map<String, String> names = getNames();

        String listenerClassName = names.get(LIQUIBASE_LISTENER_CLASS_NAME);

        for (String configurationName : configurers.keySet())
        {
            Map<String, String> configuration = getConfiguration(names);

            try
            {
                logger.info("Processing configuration '" + configurationName + "'");

                for (LiquibaseConfigurer configurer : configurers.get(configurationName))
                {
                    configurer.configure(configuration);
                }

                validate(configuration);

                apply(listenerClassName, configuration);

                logger.info("Configuration '" + configurationName + "' succeeded");
            }
            catch (RuntimeException e)
            {
                throw new RuntimeException("Configuration '" + configurationName + "' failed", e);
            }
        }

        initializer.initializeApplication(context);
    }

    private void apply(String listenerClassName, Map<String, String> configuration)
    {
        ServletContext servletContext = applicationGlobals.getServletContext();

        try
        {
            Class<?> listenerClass = Class.forName(listenerClassName);
            ServletContextListener listener =
                    (ServletContextListener) listenerClass.newInstance();

            ServletContext decoratedContext = decorate(servletContext, configuration);

            listener.contextInitialized(new ServletContextEvent(decoratedContext));
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private ServletContext decorate(
            final ServletContext servletContext,
            final Map<String, String> configuration)
    {
        try
        {
            return (ServletContext) Proxy.getProxyClass(
                    ServletContext.class.getClassLoader(),
                    new Class[] { ServletContext.class })
                    .getConstructor(new Class[] { InvocationHandler.class })
                    .newInstance(new Object[] { new InvocationHandler()
                    {
                        @SuppressWarnings("unchecked")
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args)
                                throws Throwable
                        {
                            switch (method.getName())
                            {
                            case "getInitParameterNames":
                                // getInitParameterNames(): Enumeration<String>

                                // Take all initialization parameter names from ServletContext,
                                // plus additional keys from configuration
                                List<String> names = new ArrayList<>();

                                names.addAll(Collections.list(
                                        (Enumeration<String>) method.invoke(servletContext, args)));

                                for (String key : configuration.keySet())
                                {
                                    if (!names.contains(key))
                                    {
                                        names.add(key);
                                    }
                                }

                                return Collections.enumeration(names);

                            case "getInitParameter":
                                // getInitParameter(String key): String

                                // Configuration values take precedence
                                if (configuration.containsKey(args[0]))
                                {
                                    return configuration.get(args[0]);
                                }

                                return (String) method.invoke(servletContext, args);

                            default:
                                return method.invoke(servletContext, args);
                            }
                        }
                    } });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void validate(Map<String, String> configuration)
    {
        validateRequired(configuration, LiquibaseModule.LIQUIBASE_CHANGELOG);
        validateRequired(configuration, LiquibaseModule.LIQUIBASE_DATA_SOURCE);
    }

    private void validateRequired(
            Map<String, String> configuration,
            String propertyName)
    {
        if (isEmpty(configuration.get(propertyName)))
        {
            throw new RuntimeException(MessageFormat.format(
                    "Missing required value for \"{0}\"",
                    propertyName));
        }
    }

    private Map<String, String> getConfiguration(Map<String, String> names)
    {
        Map<String, String> configuration = new HashMap<>();

        addParameter(configuration, names, LiquibaseModule.LIQUIBASE_CHANGELOG);
        addParameter(configuration, names, LiquibaseModule.LIQUIBASE_DATA_SOURCE);
        addParameter(configuration, names, LiquibaseModule.LIQUIBASE_HOST_EXCLUDES);
        addParameter(configuration, names, LiquibaseModule.LIQUIBASE_HOST_INCLUDES);
        addParameter(configuration, names, LiquibaseModule.LIQUIBASE_FAIL_ON_ERROR);
        addParameter(configuration, names, LiquibaseModule.LIQUIBASE_CONTEXTS);

        return configuration;
    }

    private void addParameter(
            Map<String, String> configuration,
            Map<String, String> names,
            String symbol)
    {
        String name = names.get(symbol);

        String value = symbolSource.valueForSymbol(symbol);

        if (!isEmpty(value))
        {
            if (symbol.equals(LiquibaseModule.LIQUIBASE_DATA_SOURCE))
            {
                if (value.startsWith("jdbc/"))
                {
                    value = "java:comp/env/" + value;
                }
            }

            configuration.put(name, value);
        }
    }

    private boolean isEmpty(String value)
    {
        return value == null || value.trim().length() == 0;
    }

}
