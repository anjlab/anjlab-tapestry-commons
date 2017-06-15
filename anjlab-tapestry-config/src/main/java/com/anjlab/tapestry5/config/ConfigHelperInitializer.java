/**
 * Copyright 2017 AnjLab
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
package com.anjlab.tapestry5.config;

import org.apache.tapestry5.TapestryFilter;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.UsesOrderedConfiguration;
import org.apache.tapestry5.services.ApplicationInitializer;
import org.apache.tapestry5.services.ApplicationInitializerFilter;
import org.apache.tapestry5.services.Context;

import java.util.ArrayList;
import java.util.List;

@UsesOrderedConfiguration(ConfigHelperValidator.class)
public class ConfigHelperInitializer implements ApplicationInitializerFilter
{
    private final List<ConfigHelperValidator> validators;

    public ConfigHelperInitializer(List<ConfigHelperValidator> validators)
    {
        this.validators = new ArrayList<ConfigHelperValidator>(validators);
    }

    @Override
    public void initializeApplication(Context context, ApplicationInitializer initializer)
    {
        ObjectLocator objectLocator = (ObjectLocator) context.getAttribute(TapestryFilter.REGISTRY_CONTEXT_NAME);

        ConfigHelper configHelper = objectLocator.getService(ConfigHelper.class);

        for (ConfigHelperValidator validator : validators)
        {
            validator.validate(configHelper);
        }

        initializer.initializeApplication(context);
    }
}
