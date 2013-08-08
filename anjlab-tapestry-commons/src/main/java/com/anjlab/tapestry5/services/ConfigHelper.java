package com.anjlab.tapestry5.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHelper
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigHelper.class);
    
    private Properties properties;
    
    public static ConfigHelper fromClasspathResource(String resourceName) throws IOException
    {
        logger.info("Reading config from classpath: {}", resourceName);
        InputStream input = ConfigHelper.class.getClassLoader().getResourceAsStream(resourceName);
        if (input == null)
        {
            throw new IOException("Classpath resource not found: " + resourceName);
        }
        return new ConfigHelper(new AutoCloseInputStream(input));
    }
    
    public static ConfigHelper fromSystemProperty(String property) throws IOException
    {
        String filename = System.getProperty(property);
        logger.info("Reading config from system property {}={}", property, filename);
        return new ConfigHelper(filename);
    }
    
    public static ConfigHelper fromFile(String file) throws IOException
    {
        return new ConfigHelper(file);
    }
    
    public static ConfigHelper fromFile(File file) throws IOException
    {
        return new ConfigHelper(file);
    }
    
    public ConfigHelper(String configFilename) throws IOException
    {
        this(new File(configFilename));
    }
    
    private ConfigHelper(File configFile) throws IOException
    {
        logger.info("Reading config from file: {}", configFile.getAbsoluteFile());
        readProperties(configFile);
    }
    
    public static ConfigHelper fromStream(InputStream input) throws IOException
    {
        return new ConfigHelper(input);
    }
    
    private ConfigHelper(InputStream input) throws IOException
    {
        readProperties(input);
    }
    
    protected void readProperties(File configFile) throws IOException
    {
        if (!configFile.exists())
        {
            throw new IllegalStateException("Configuration file not found: "
                    + configFile.getAbsolutePath());
        }
        
        FileInputStream input = null;
        try
        {
            input = new FileInputStream(configFile);
            
            readProperties(input);
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                }
            }
        }
    }
    
    protected void readProperties(InputStream input) throws IOException
    {
        properties = new Properties();
        properties.load(input);
    }
    
    public void addIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            configuration.add(propertyName, properties.get(propertyName));
        }
    }
    
    public void overrideIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            configuration.override(propertyName, properties.get(propertyName));
        }
    }
    
    public void override(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        assertPropertyDefined(propertyName, properties);

        configuration.override(propertyName, properties.get(propertyName));
    }
    
    public void add(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        assertPropertyDefined(propertyName, properties);

        configuration.add(propertyName, properties.get(propertyName));
    }
    
    public String get(String propertyName)
    {
        return properties.getProperty(propertyName);
    }
    
    private static void assertPropertyDefined(String propertyName, Properties properties)
    {
        if (!properties.containsKey(propertyName))
        {
            throw new IllegalStateException("Required property not defined: " + propertyName);
        }
    }

}