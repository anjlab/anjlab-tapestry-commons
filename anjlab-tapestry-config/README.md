Configuration helpers for Tapestry5
===================================

`ConfigHelper` is a helper class built for Tapestry IoC's [`MappedConfiguration`](http://tapestry.apache.org/tapestry-ioc-configuration.html).

`ConfigHelper` can read data from a properties file on disk or classpath.


The purpose of ConfigHelper is to fail application start if required property not found in the properties file, or if the configuration isn't valid (read about validation below).

### Usage

1. Define source of the properties by contributing instance of `ConfigHelper` class to the `ConfigHelper` service:

    ```java
    @Contribute(ConfigHelper.class)
    public void contributeConfigHelper(OrderedConfiguration<ConfigHelper> configuration) throws IOException
    {
        configuration.add("Config", ConfigHelper.fromClasspathResource("config.properties"));
    }
    ```

   This may look like a chicken-egg problem where in order to configure the service you
    first create an instance of service's class.

    You can contribute as many `ConfigHelper`s as you like, they will extend each other
     in specified order by overwriting repeated properties.

2. Contribute application symbols using `ConfigHelper`:

    ```java
    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration,
            ObjectLocator objectLocator)
    {
       ConfigHelper configHelper = objectLocator.getService(ConfigHelper.class);

       configHelper.add("property1", configuration);
       configHelper.addIfExists("property2", configuration);
       configHelper.override("property3", configuration);
       configHelper.overrideIfExists("property4", configuration);
    }
    ```
    
    `.add` and `.override` methods will fail if no property was found in `ConfigHelper`.
    
    You can also use `ConfigHelper.names()` to get names of all available properties,
    and `ConfigHelper.get(String propertyName)` to get raw value of a property.

#### Referencing properties files from each other

In addition to contributing multiple `ConfigHelper` configurations is also possible
to reference another properties file by using special property name -- `extend`.

I.e. if you have these two files:

*base-config.properties*
```
prop1=Base Property 1
prop2=Base Property 2
```

*extending-config.properties*
```
extend=base-config.properties
prop1=Overridden Property 1
```

Creating `ConfigHelper` from *extending-config.properties* will automatically load *base-config.properties*. Resulting `ConfigHelper` will have property `prop1` with value `Overridden Property 1`.

It's also possible to extend from more than one file, in this case you need to specify the order explicitly: 

*multi-extending-config.properties*
```
extend.1=extending-config.properties
extend.2=base-config.properties
```

In this case *extending-config.properties* will be loaded first, and its only defined property `prop1` will then be overwritten with value from `base-config.properties`.


#### Validating configuration properties

`ConfigHelperModule` registers itself as Tapestry5 `ApplicationInitializerFilter`
that runs `ConfigHelperValidator`s before application started serving requests.

There's just one built-in validator named `"UnreferencedProperties"` that reports properties that were not referenced during application configuration.

To add your own validator create a class that extends `ConfigHelperValidator` and contribute it to `ConfigHelperInitializer`:

```java
public void contributeConfigHelperInitializer(
            OrderedConfiguration<ConfigHelperValidator> configuration)
{
    configuration.addInstance("CustomConfigValidator", CustomConfigValidator.class);
}
```

#### Changing order of `ConfigHelperInitializer`

It may be necessary to change the order of config validation, i.e. if you need to read values from database, but you want to apply Liquibase migrations first.

If you're using `anjlab-tapestry-liquibase` the following configuration should do it:
```java
public void contributeApplicationInitializer(
        OrderedConfiguration<ApplicationInitializerFilter> configuration,
        @Inject @Symbol(LIQUIBASE_SHOULD_RUN) boolean shouldRunLiquibase,
        ConfigHelperInitializer configHelperInitializer)
{
    if (shouldRunLiquibase)
    {
        configuration.override("ConfigHelper", configHelperInitializer, "after:Liquibase");
    }
}
```

### Installation
This JAR is a Tapestry5 drop-in module and it will register itself if you 
add `anjlab-tapestry-config` as a dependency to your app using your build tool
and [this repository](https://github.com/anjlab/anjlab-tapestry-commons#maven-repository):

#### Maven
```xml
<dependency>
    <groupId>com.anjlab.tapestry5</groupId>
    <artifactId>anjlab-tapestry-config</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```groovy
compile 'com.anjlab.tapestry5:anjlab-tapestry-config:1.0.0'
```
