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

import com.anjlab.tapestry5.config.ConfigHelper;
import com.anjlab.tapestry5.config.PropertyTypeValidator;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Properties;

public class PropertyTypeValidatorTest
{
    @Test
    public void shouldSkipValidationOfUnreferencedProperties()
    {
        Properties properties = new Properties();
        properties.put("property", "false");

        ConfigHelper configHelper = new ConfigHelper(properties);

        PropertyTypeValidator validator = new PropertyTypeValidator(null, null);

        validator.validate(configHelper);
    }

    @Test
    public void shouldSkipValidationOfPropertiesWithDefaultType()
    {
        Properties properties = new Properties();
        properties.put("property", "false");

        MappedConfiguration<String, Object> configuration = Mockito.mock(MappedConfiguration.class);

        ConfigHelper configHelper = new ConfigHelper(properties);
        configHelper.add("property", configuration);

        Mockito.inOrder(configuration)
                .verify(configuration, Mockito.calls(1))
                .add("property", "false");

        PropertyTypeValidator validator = new PropertyTypeValidator(null, null);

        validator.validate(configHelper);
    }

    @Test
    public void shouldUseTypeCoercerWithValueFromSymbolSourceToValidatePropertyType()
    {
        Properties properties = new Properties();
        properties.put("property", "${symbol}");

        MappedConfiguration<String, Object> configuration = Mockito.mock(MappedConfiguration.class);
        TypeCoercer typeCoercer = Mockito.mock(TypeCoercer.class);
        SymbolSource symbolSource = Mockito.mock(SymbolSource.class);

        Mockito.when(symbolSource.expandSymbols("${symbol}")).thenReturn("true");

        ConfigHelper configHelper = new ConfigHelper(properties);
        configHelper.add(Boolean.class, "property", configuration);

        Mockito.inOrder(configuration)
                .verify(configuration, Mockito.calls(1))
                .add("property", "${symbol}");

        PropertyTypeValidator validator = new PropertyTypeValidator(typeCoercer, symbolSource);

        validator.validate(configHelper);

        Mockito.inOrder(typeCoercer)
                .verify(typeCoercer, Mockito.calls(1))
                .coerce("true", Boolean.class);
    }
}
