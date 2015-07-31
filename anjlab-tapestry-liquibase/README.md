Liquibase for Tapestry5
=======================

Use LiquibaseModule to apply [Liquibase](http://www.liquibase.org) changesets to your database.

LiquibaseModule registers itself as Tapestry5 initializer filter and 
runs liquibase changesets before your application starts serving requests.

### Usage

It is a [best practice](http://www.liquibase.org/bestpractices.html) to organize your liquibase changesets using a single
file `db.changelog-master.xml` that includes links to actual changeset files:
``` xml
<?xml version="1.0" encoding="UTF-8"?> 
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-2.0.xsd
    http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd">
    
    <include file="db.changelog-1.0.xml" relativeToChangelogFile="true" />
    <include file="db.changelog-1.1.xml" relativeToChangelogFile="true" />
    <include file="db.changelog-1.2.xml" relativeToChangelogFile="true" />
    
</databaseChangeLog>
```

You can store these changesets as your project's resources so that they got into classpath:
```
└── src
    ├── main
    │   └── resources
    │       └── liquibase
    │           ├── db.changelog-1.0.xml
    │           ├── db.changelog-1.1.xml
    │           ├── db.changelog-1.2.xml
    │           └── db.changelog-master.xml
```

You can tell liquibase to run these changesets by contributing parameters to application defaults:
``` java
    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        configuration.add(LiquibaseModule.LIQUIBASE_CHANGELOG, "liquibase/db.changelog-master.xml");
        //  Usually the same DataSource as in web.xml, but you may want to run changesets using
        //  a different connection as a user with priviledges to run DDL statements
        configuration.add(LiquibaseModule.LIQUIBASE_DATA_SOURCE, "jdbc/my-db");
    }
```

### Configuring LiquibaseModule

This module uses [Liquibase servlet listener](http://www.liquibase.org/documentation/servlet_listener.html)
to run the changesets.

LiquibaseModule unifies configuration parameters to support both Liquibase 1.9.x and 2.x:

``` java
    public static final String LIQUIBASE_VERSION = "liquibase.version";
    
    public static final String LIQUIBASE_CHANGELOG = "liquibase.changelog";
    public static final String LIQUIBASE_DATA_SOURCE = "liquibase.datasource";
    public static final String LIQUIBASE_HOST_EXCLUDES = "liquibase.host.excludes";
    public static final String LIQUIBASE_HOST_INCLUDES = "liquibase.host.includes";
    public static final String LIQUIBASE_FAIL_ON_ERROR = "liquibase.onerror.fail";
    public static final String LIQUIBASE_CONTEXTS = "liquibase.contexts";
    
    public static final String LIQUIBASE_SHOULD_RUN = "liquibase.should.run";
```

Please refer to [official documentation](http://www.liquibase.org/documentation/servlet_listener.html)
for description of configuration parameters.

The only LiquibaseModule-specific parameter is `LIQUIBASE_VERSION` which you should set
to `LIQUIBASE_VERSION_2_X` or `LIQUIBASE_VERSION_1_9_X` depending on what version of Liquibase you use.

Default parameter values are:
``` java
    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        configuration.add(LIQUIBASE_VERSION, LIQUIBASE_VERSION_2_X);
        
        configuration.add(LIQUIBASE_FAIL_ON_ERROR, "true");
        configuration.add(LIQUIBASE_SHOULD_RUN, "true");
        
        // ...
    }
```

### Auto-configure Liquibase DataSource with Hibernate JPA
Liquibase may use the same connection settings as Hibernate if you use it as JPA provider with official [`tapestry-jpa` integration](https://tapestry.apache.org/integrating-with-jpa.html).

In order to do this add `AutoConfigureLiquibaseDataSourceFromHibernateJPAModule` class as a submodule to your `AppModule`:
``` java
@SubModule({
        AutoConfigureLiquibaseDataSourceFromHibernateJPAModule.class
})
public class AppModule
```

By default it will take settings from the first persistence unit declared in your `persistence.xml`.

You can also specify a name of persistence unit explicitly:
``` java
    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        configuration.add(
            AutoConfigureLiquibaseDataSourceFromHibernateJPAModule.LIQUIBASE_PERSISTENCE_UNIT_NAME,
            "my-pu");
    }
```

### Installation
This JAR is a Tapestry5 drop-in module and it will register itself if you 
add `anjlab-tapestry-liquibase` as a dependency to your app using your build tool
and [this repository](https://github.com/anjlab/anjlab-tapestry-commons#maven-repository):

#### Maven
```xml
<dependency>
    <groupId>com.anjlab.tapestry5</groupId>
    <artifactId>anjlab-tapestry-liquibase</artifactId>
    <version>1.3.0</version>
</dependency>
```

#### Gradle
```groovy
compile 'com.anjlab.tapestry5:anjlab-tapestry-liquibase:1.3.0'
```
