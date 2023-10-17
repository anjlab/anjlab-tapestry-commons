/**
 * Copyright 2017 AnjLab
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
package com.anjlab.tapestry5.config;

import org.apache.tapestry5.commons.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.http.services.ApplicationInitializerFilter;

public class ConfigHelperModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(ConfigHelper.class);
        binder.bind(ConfigHelperInitializer.class);
    }

    public void contributeApplicationInitializer(
            OrderedConfiguration<ApplicationInitializerFilter> configuration,
            ConfigHelperInitializer configHelperInitializer)
    {
        configuration.add("ConfigHelper", configHelperInitializer);
    }

    public void contributeConfigHelperInitializer(
            OrderedConfiguration<ConfigHelperValidator> configuration)
    {
        configuration.addInstance("UnreferencedProperties", UnreferencedPropertiesValidator.class);
        configuration.addInstance("PropertyType", PropertyTypeValidator.class);
    }
}
