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

import org.slf4j.Logger;

import java.util.Set;

/**
 * Find all unreferenced properties in {@link ConfigHelper} and print names of the properties using {@link Logger}.
 *
 * @see ConfigHelper#getReferenced()
 */
public class UnreferencedPropertiesValidator implements ConfigHelperValidator
{
    private final Logger logger;

    public UnreferencedPropertiesValidator(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void validate(ConfigHelper configHelper)
    {
        Set<String> names = configHelper.getPropertyNames();

        names.removeAll(configHelper.getReferenced());

        for (String propertyName : names)
        {
            logger.info("Property '{}' is defined in configuration but was never referenced", propertyName);
        }
    }
}
