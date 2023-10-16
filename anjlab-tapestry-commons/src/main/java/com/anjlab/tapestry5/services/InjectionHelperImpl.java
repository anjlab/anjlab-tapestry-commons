/**
 * Copyright 2015 AnjLab
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
package com.anjlab.tapestry5.services;

import org.apache.tapestry5.ioc.ObjectCreator;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.OperationTracker;
import org.apache.tapestry5.ioc.ReloadAware;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.internal.OperationTrackerImpl;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.internal.util.InjectionResources;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.ioc.internal.util.MapInjectionResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InjectionHelperImpl implements InjectionHelper, ReloadAware
{
    private static final Logger logger = LoggerFactory.getLogger(InjectionHelperImpl.class);

    @Inject
    private ObjectLocator locator;

    private final Map<Class<?>, Method> postInjectionMethodsCache =
            new ConcurrentHashMap<Class<?>, Method>();

    @Override
    public void inject(Object target)
    {
        injectFields(target);
        try
        {
            invokePostInjection(target, false);
        }
        catch (NoSuchMethodException e)
        {
            // This exception shouldn't ever be thrown,
            // but we still re-throw it just to verify it isn't there in tests
            throw new RuntimeException(e);
        }
    }

    @Override
    public void injectFields(Object target)
    {
        InternalUtils.injectIntoFields(
                target, locator, createInjectionResources(), createOperationTracker());
    }

    @Override
    public void invokePostInjection(Object target) throws NoSuchMethodException
    {
        invokePostInjection(target, true);
    }

    private void invokePostInjection(Object target, boolean failIfMethodMissing)
            throws NoSuchMethodException
    {
        Class<?> targetClass = target.getClass();

        final Method method;

        if (postInjectionMethodsCache.containsKey(targetClass))
        {
            method = postInjectionMethodsCache.get(targetClass);
        }
        else
        {
            method = findPostInjectionMethod(targetClass);

            if (method == null)
            {
                if (failIfMethodMissing)
                {
                    throw new NoSuchMethodException(targetClass.getName()
                            + " doesn't have public methods annotated with @PostInjection or @PostConstruct");
                }

                return;
            }

            postInjectionMethodsCache.put(targetClass, method);
        }

        if (method != null)
        {
            invokeMethodWithParameters(target, method);
        }
    }

    private void invokeMethodWithParameters(Object target, Method method)
    {
        ObjectCreator<?>[] parameters = InternalUtils.calculateParametersForMethod(
                method, locator, createInjectionResources(), createOperationTracker());

        try
        {
            method.invoke(target, InternalUtils.realizeObjects(parameters));
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    private OperationTracker createOperationTracker()
    {
        return new OperationTrackerImpl(logger);
    }

    private InjectionResources createInjectionResources()
    {
        @SuppressWarnings("rawtypes")
        Map<Class, Object> resourceMap = CollectionFactory.newMap();

        resourceMap.put(ObjectLocator.class, locator);
        resourceMap.put(Logger.class, logger);

        InjectionResources injectionResources = new MapInjectionResources(resourceMap);
        return injectionResources;
    }

    private Method findPostInjectionMethod(Class<?> targetClass)
    {
        for (Method method : targetClass.getMethods())
        {
            if (method.getAnnotation(PostInjection.class) != null
                    || method.getAnnotation(PostConstruct.class) != null)
            {
                return method;
            }
        }
        return null;
    }

    @Override
    public boolean shutdownImplementationForReload()
    {
        postInjectionMethodsCache.clear();
        return true;
    }

}
