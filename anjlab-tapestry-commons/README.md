Commons for Tapestry5
=====================

Set of useful classes that might be helpful for Tapestry5 projects.

No Tapestry-IoC drop-in modules here, just use these classes as you want.

Usually you will register them as services in your AppModule manually.

[Publisher API](https://github.com/anjlab/anjlab-tapestry-commons/wiki/Publisher-API)

```java
    public static void bind(ServiceBinder binder)
    {
        binder.bind(Publisher.class);
        ...
    }
```


More documentation will follow... :)

### Installation
Add `anjlab-tapestry-commons` as a dependency to your app using your build tool
and [this repository](https://github.com/anjlab/anjlab-tapestry-commons#maven-repository):

#### Maven
```xml
<dependency>
    <groupId>com.anjlab.tapestry5</groupId>
    <artifactId>anjlab-tapestry-commons</artifactId>
    <version>1.2.3</version>
</dependency>
```

#### Gradle
```groovy
compile 'com.anjlab.tapestry5:anjlab-tapestry-commons:1.2.3'
```
