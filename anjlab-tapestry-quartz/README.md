Quartz for Tapestry5
====================

This module allows you to use [Quartz scheduler](http://quartz-scheduler.org) in your Tapestry5 applications.

You may use it instead of [ParallelExecutor](http://tapestry.apache.org/parallel-execution.html).

### Installation

This JAR is a Tapestry5 drop-in module and it will register itself if you 
add `anjlab-tapestry-quartz` as a dependency to your app using your build tool
and [this repository](https://github.com/anjlab/anjlab-tapestry-commons#maven-repository):

#### Maven
```xml
<dependency>
    <groupId>com.anjlab.tapestry5</groupId>
    <artifactId>anjlab-tapestry-quartz</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```groovy
compile 'com.anjlab.tapestry5:anjlab-tapestry-quartz:1.0.0'
```

### Usage

 1. Define your job class:
   - this may be simple POJO;
   - this doesn't have to be registered as a Tapestry5 service;
   - you may use `@Inject` in this class as you usually do in Tapestry5 pages and services;
   - instances of this job class will be created using <a href="http://tapestry.apache.org/current/apidocs/org/apache/tapestry5/ioc/ObjectLocator.html#autobuild(java.lang.Class)">Registry.autobuild()</a> method;
   - plus you will get the same behaviour as Quartz's default JobFactory:

     > If you add setter methods to your job class that correspond to the names of keys
     > in the JobDataMap (such as a setJobSays(String val) method for the data in the example above),
     > then Quartz's default JobFactory implementation will automatically call those setters
     > when the job is instantiated, thus preventing the need to explicitly get the values out
     > of the map within your execute method.
     >
     > http://quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/tutorial-lesson-03


   ``` java
   public class HelloJob implements org.quartz.Job
   {
       private static final Logger logger = LoggerFactory.getLogger(HelloJob.class);
       
       private String username;
       
       @Inject @Symbol(SymbolConstants.APPLICATION_VERSION)
       private String version;
       
       @Override
       public void execute(JobExecutionContext context) throws JobExecutionException
       {
           String message = String.format("username = %s, app version = %s", username, version);
           
           logger.info(message);
           
           context.setResult(message);
       }
       
       public void setUsername(String username)
       {
           this.username = username;
       }
   }
   ```

 2. `@Inject` instance of `org.quartz.Scheduler` to your page/service and schedule the job for execution:

    ``` java
        JobDetail job = JobBuilder.newJob(HelloJob.class).build();
        
        job.getJobDataMap().put("username", "John Smith");
        
        Trigger trigger = TriggerBuilder.newTrigger().startNow().build();
        
        scheduler.scheduleJob(job, trigger);

    ```
    
    For more details on defining and scheduling jobs refer to [Quartz documentation](http://quartz-scheduler.org/documentation/quartz-2.2.x/tutorials).
    
### Support for Unit-Testing

You may use `QuartzJobFuture` class to wait until the job finish its execution:

```java
QuartzJobFuture<String> future = new QuartzJobFuture<String>(scheduler, job.getKey());

scheduler.scheduleJob(job, trigger);

String result = future.get(5, TimeUnit.SECONDS);

// ...

Assert.assertEquals("username = John Smith, app version = " + appVersion, result);
```

See `SchedulerTest` for example of using `QuartzJobFuture`.

### Configuring Quartz

Look at the source code of `QuartzModule.java` to see default Quartz settings. Here they are:

``` java
    public static void contributeSchedulerFactory(MappedConfiguration<String, Object> configuration)
    {
        configuration.add("org.quartz.scheduler.skipUpdateCheck", "true");
        configuration.add("org.quartz.scheduler.instanceName", "TapestryQuartz");
        configuration.add("org.quartz.threadPool.threadCount", "3");
        configuration.add("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
    }
```

You may contribute/override to these settings as you usually do for other Tapestry5 IoC services in your `AppModule.java`.

Quartz configuration reference is [here](http://quartz-scheduler.org/documentation/quartz-2.2.x/configuration/).

You may also load Quartz properties from external resource by contributing
`"quartz.properties"` configuration parameter, like this:

``` java
    public static void contributeSchedulerFactory(MappedConfiguration<String, Object> configuration)
    {
        configuration.add("quartz.properties", "/path/to/quartz.properties");
    }
```

In this case only configuration parameters from this file will be used.

The value of the `"quartz.properties"` parameter may be of type:
  - `java.lang.String` (which is the path to quartz.properties file)
  - `java.util.Properties`
  - `java.io.InputStream`
  - `org.apache.tapestry5.ioc.Resource`


Application symbol exists to control Quartz scheduler shutdown policy (default value is `false`):

```
QuartzModule.WAIT_FOR_JOBS_TO_COMPLETE="org.quartz.scheduler.waitForJobsToComplete"
```

<a href="http://quartz-scheduler.org/api/2.2.0/org/quartz/Scheduler.html#shutdown(boolean)">See here</a> for details.
