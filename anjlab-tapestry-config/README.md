Configuration helpers for Tapestry5
===================================

`ConfigHelper` is a helper class and a Tapestry-IoC-service built around [`MappedConfiguration`](http://tapestry.apache.org/tapestry-ioc-configuration.html).

The purpose of `ConfigHelper` is to validate application properties early on application initialization and fail application start before it starts serving user requests if configuration was not valid.

`ConfigHelper` uses format of `java.util.Properties` to define application properties.

### Usage

1. Define source of the properties by contributing instance of `ConfigHelper` class to the `ConfigHelper` Tapestry-IoC-service:

    ```java
    @Contribute(ConfigHelper.class)
    public void contributeConfigHelper(OrderedConfiguration<ConfigHelper> configuration) throws IOException
    {
        configuration.add("MyConfig", ConfigHelper.fromClasspathResource("my-config.properties"));
    }
    ```

   `ConfigHelper` as-a-Tapestry-IoC-service takes instances of `ConfigHelper` as-plain-old-java-objects to configure itself. There are number of ways you can create instances of `ConfigHelper` objects, i.e. from `File`, from classpath resource, or simply from `Properties` object.

    You can contribute as many `ConfigHelper`s to service configuration as you like, they will extend each other in specified order by overwriting repeated properties.

2. Contribute application symbols using `ConfigHelper`:

    ```java
    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration,
            ObjectLocator objectLocator)
    {
       ConfigHelper configHelper = objectLocator.getService(ConfigHelper.class);

       configHelper.add("property1", configuration);
       configHelper.add(Integer.class, "property2", configuration);
       configHelper.addIfExists("property3", configuration);
       configHelper.addIfExists(Boolean.class, "property4", configuration);

       configHelper.override("property5", configuration);
       configHelper.override(Date.class, "property6", configuration);
       configHelper.overrideIfExists("property7", configuration);
       configHelper.overrideIfExists(Money.class, "property8", configuration);
    }
    ```
    
    `.add...` and `.override...` methods will fail if no property was found in `ConfigHelper`.
    
    Note there are overloads of `.add...` and `.override...` methods that accept property type as first argument. This type will be used by the `PropertyTypeValidator` to check that property value (after symbol expansion) can be coerced to desired type. Validator will use Tapestry's `TypeCoercer`, you will need to implement and contribute a custom `TypeCoercer` for your custom types.
    
    You can also use `ConfigHelper.getPropertyNames()` to get names of all available properties,
    and `ConfigHelper.getRaw(String propertyName)` to get raw value of a property (before symbol expansion).

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

##### Extending multiple files

It's also possible to extend from more than one file, in this case you need to specify the order explicitly: 

*multi-extending-config.properties*
```
extend.1=extending-config.properties
extend.2=base-config.properties
```

In this case *extending-config.properties* will be loaded first, and its only defined property `prop1` will then be overwritten with value from `base-config.properties`.

##### Loading property names with custom prefix/namespace

You can specify optional prefix for property names when extending configs using special property name --
`extend.prefix` (or `extend.N.prefix` for ordered extensions), i.e.:

*extend-with-prefix.properties*
```
extend=base-config.properties
extend.prefix=namespace.

prop1=Preventing name clashes

namespace.prop1=Overridden Property 1
```

Resulting `ConfigHelper` will contain the following properties:

```
prop1=Preventing name clashes
namespace.prop1=Overridden Property 1
namespace.prop2=Base Property 2
```

#### Validating configuration properties

`ConfigHelperModule` registers itself as Tapestry5 `ApplicationInitializerFilter`
that runs `ConfigHelperValidator`s before application started serving requests.

Built-in validators:
 - `"UnreferencedProperties"` reports properties that were not referenced during application configuration;
 - `"PropertyType"` validates types of property values after symbol expansion using `TypeCoercer`.

To add your own validator create a class that extends `ConfigHelperValidator` and contribute it to `ConfigHelperInitializer`:

```java
public void contributeConfigHelperInitializer(
            OrderedConfiguration<ConfigHelperValidator> configuration)
{
    configuration.addInstance("MyConfigValidator", MyConfigValidator.class);
}
```

#### Changing order of `ConfigHelperInitializer`

It may be necessary to change the order of config validation, i.e. if you need to read some values from a database, but only after Liquibase change sets were applied.

If you're using `anjlab-tapestry-liquibase` the following configuration should do it:
```java
public void contributeApplicationInitializer(
        OrderedConfiguration<ApplicationInitializerFilter> configuration,
        @Inject @Symbol(LiquibaseModule.LIQUIBASE_SHOULD_RUN) boolean shouldRunLiquibase,
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
    <version>1.0.1</version>
</dependency>
```

#### Gradle
```groovy
compile 'com.anjlab.tapestry5:anjlab-tapestry-config:1.0.1'
```
