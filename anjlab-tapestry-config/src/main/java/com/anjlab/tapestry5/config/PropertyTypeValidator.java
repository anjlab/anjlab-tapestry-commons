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

import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.commons.services.TypeCoercer;

/**
 * Validates types of property values after symbol expansion using {@link TypeCoercer}.
 * <p>
 * This will mark all properties as referenced, so {@link UnreferencedPropertiesValidator} should be invoked before this.
 */
public class PropertyTypeValidator implements ConfigHelperValidator
{
    private final TypeCoercer typeCoercer;
    private final SymbolSource symbolSource;

    public PropertyTypeValidator(TypeCoercer typeCoercer, SymbolSource symbolSource)
    {
        this.typeCoercer = typeCoercer;
        this.symbolSource = symbolSource;
    }

    @Override
    public void validate(ConfigHelper configHelper) throws RuntimeException
    {
        for (String propertyName : configHelper.getPropertyNames())
        {
            Class<?> desiredType = configHelper.getPropertyType(propertyName);

            if (desiredType == null)
            {
                //  Unreferenced property -- no type information, skip validation
                continue;
            }

            if (desiredType == Object.class)
            {
                //  Object.class is the default, skip validation
                continue;
            }

            String propertyValue = configHelper.getRaw(propertyName);

            String symbolValue = symbolSource.expandSymbols(propertyValue);

            //  This will throw RuntimeException if type can't be coerced
            typeCoercer.coerce(symbolValue, desiredType);
        }
    }
}
