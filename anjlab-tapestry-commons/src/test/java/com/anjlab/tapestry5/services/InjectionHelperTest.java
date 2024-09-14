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

import jakarta.annotation.PostConstruct;
import org.apache.tapestry5.commons.ObjectLocator;
import org.apache.tapestry5.http.modules.TapestryHttpModule;
import org.apache.tapestry5.http.services.RequestGlobals;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class InjectionHelperTest
{

    protected static Registry registry;

    @BeforeClass
    public static void setup()
    {
        registry = new RegistryBuilder().add(
                        TapestryHttpModule.class,
                        CommonsModule.class)
                .build();

        registry.performRegistryStartup();
    }

    public static final class PostInjectionTestTarget
    {
        public InjectionHelper injectionHelper;
        public ObjectLocator objectLocator;
        public RequestGlobals requestGlobals;

        @PostInjection
        public void postInjection(
                ObjectLocator objectLocator,
                RequestGlobals requestGlobals,
                InjectionHelper injectionHelper)
        {
            // Just some random services for test
            this.objectLocator = objectLocator;
            this.requestGlobals = requestGlobals;
            // Also test that we can inject the injectionHelper
            this.injectionHelper = injectionHelper;
        }
    }

    public static final class FieldsInjectionTestTarget
    {
        @Inject
        public InjectionHelper injectionHelper;
        @Inject
        public ObjectLocator objectLocator;
        @Inject
        public RequestGlobals requestGlobals;
    }

    public static final class BothInjectionsTestTarget
    {
        @Inject
        public InjectionHelper injectionHelper;
        @Inject
        public ObjectLocator objectLocator;

        public RequestGlobals requestGlobals;

        @PostConstruct
        public void postInjection(
                RequestGlobals requestGlobals)
        {
            this.requestGlobals = requestGlobals;
        }
    }

    @Test
    public void testPostInjection() throws NoSuchMethodException
    {
        PostInjectionTestTarget target = new PostInjectionTestTarget();

        registry.getService(InjectionHelper.class).invokePostInjection(target);

        Assert.assertNotNull(target.objectLocator);
        Assert.assertNotNull(target.requestGlobals);
        Assert.assertNotNull(target.injectionHelper);
    }

    @Test
    public void testFieldInjection()
    {
        FieldsInjectionTestTarget target = new FieldsInjectionTestTarget();

        registry.getService(InjectionHelper.class).injectFields(target);

        Assert.assertNotNull(target.objectLocator);
        Assert.assertNotNull(target.requestGlobals);
        Assert.assertNotNull(target.injectionHelper);
    }

    @Test
    public void testBothInjections()
    {
        BothInjectionsTestTarget target = new BothInjectionsTestTarget();

        registry.getService(InjectionHelper.class).inject(target);

        Assert.assertNotNull(target.objectLocator);
        Assert.assertNotNull(target.requestGlobals);
        Assert.assertNotNull(target.injectionHelper);
    }

    @Test
    public void testInjectFieldsWhenNoFieldsDeclared()
    {
        PostInjectionTestTarget target = new PostInjectionTestTarget();

        registry.getService(InjectionHelper.class).injectFields(target);

        Assert.assertNull(target.objectLocator);
        Assert.assertNull(target.requestGlobals);
        Assert.assertNull(target.injectionHelper);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testInvokePostInjectionWhenNoMethodDeclaredShouldFailWithException()
            throws NoSuchMethodException
    {
        FieldsInjectionTestTarget target = new FieldsInjectionTestTarget();

        registry.getService(InjectionHelper.class).invokePostInjection(target);

        Assert.assertNull(target.objectLocator);
        Assert.assertNull(target.requestGlobals);
        Assert.assertNull(target.injectionHelper);
    }

    public void testInjectWhenNoMethodDeclaredShouldFailSilently()
            throws NoSuchMethodException
    {
        FieldsInjectionTestTarget target = new FieldsInjectionTestTarget();

        registry.getService(InjectionHelper.class).inject(target);

        Assert.assertNull(target.objectLocator);
        Assert.assertNull(target.requestGlobals);
        Assert.assertNull(target.injectionHelper);
    }
}
