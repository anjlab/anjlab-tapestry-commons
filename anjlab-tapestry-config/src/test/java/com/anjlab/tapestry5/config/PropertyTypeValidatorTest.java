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
