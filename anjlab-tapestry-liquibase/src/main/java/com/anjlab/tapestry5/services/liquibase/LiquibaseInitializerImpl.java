/**
 * Copyright 2013 AnjLab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.tapestry5.services.liquibase;

import java.util.HashMap;
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

public class LiquibaseInitializerImpl implements LiquibaseInitializer
{
    @Inject private ApplicationGlobals applicationGlobals;
    
    @Inject private SymbolSource symbolSource;
    
    @Inject @Symbol(LiquibaseModule.LIQUIBASE_VERSION) private String liquibaseVersion;
    
    private static final String LIQUIBASE_LISTENER_CLASS_NAME = "liquibase.listener.classname";
    
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
            throw new RuntimeException("Unsupported value of " + LiquibaseModule.LIQUIBASE_VERSION + "=" + liquibaseVersion
                    + ". Supported values are: " + LiquibaseModule.LIQUIBASE_VERSION_1_9_X + " and " + LiquibaseModule.LIQUIBASE_VERSION_2_X);
        }
        
        return names;
    }
    
    @Override
    public void initializeApplication(Context context, ApplicationInitializer initializer)
    {
        ServletContext servletContext = applicationGlobals.getServletContext();
        
        Map<String, String> names = getNames();
        
        addInitParameter(servletContext, names, LiquibaseModule.LIQUIBASE_CHANGELOG, true);
        addInitParameter(servletContext, names, LiquibaseModule.LIQUIBASE_DATA_SOURCE, true);
        addInitParameter(servletContext, names, LiquibaseModule.LIQUIBASE_HOST_EXCLUDES, false);
        addInitParameter(servletContext, names, LiquibaseModule.LIQUIBASE_HOST_INCLUDES, false);
        addInitParameter(servletContext, names, LiquibaseModule.LIQUIBASE_FAIL_ON_ERROR, false);
        addInitParameter(servletContext, names, LiquibaseModule.LIQUIBASE_CONTEXTS, false);
        
        try
        {
            Class<?> listenerClass = Class.forName(names.get(LIQUIBASE_LISTENER_CLASS_NAME));
            ServletContextListener listener = (ServletContextListener) listenerClass.newInstance();
            listener.contextInitialized(new ServletContextEvent(servletContext));
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
        
        initializer.initializeApplication(context);
    }

    private void addInitParameter(ServletContext servletContext,
            Map<String, String> names, String symbol, boolean required)
    {
        String name = names.get(symbol);
        
        String value = symbolSource.valueForSymbol(symbol);
        
        if (required && isEmpty(value))
        {
            throw new RuntimeException("Missing required value for \"" + name + "\"");
        }
        
        if (!isEmpty(value))
        {
            if (symbol.equals(LiquibaseModule.LIQUIBASE_DATA_SOURCE))
            {
                if (value.startsWith("jdbc/"))
                {
                    value = "java:comp/env/" + value;
                }
            }
            
            servletContext.setInitParameter(name, value);
        }
    }

    private boolean isEmpty(String value)
    {
        return value == null || value.trim().length() == 0;
    }

}
