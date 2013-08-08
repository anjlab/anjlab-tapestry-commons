package com.anjlab.tapestry5.services.quartz;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.TapestryFilter;
import org.apache.tapestry5.internal.test.PageTesterContext;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.services.ApplicationGlobals;
import org.apache.tapestry5.services.TapestryModule;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class SchedulerTest
{
    protected static Registry registry;

    @BeforeClass
    public static void setup()
    {
        registry = new RegistryBuilder().add(
                    TapestryModule.class,
                    QuartzModule.class)
                .build();

        ApplicationGlobals globals = registry.getObject(
                ApplicationGlobals.class, null);

        globals.storeContext(new PageTesterContext(""));
        
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        
        Mockito.when(servletContext.getAttribute(TapestryFilter.REGISTRY_CONTEXT_NAME))
               .thenReturn(registry);
        
        //  Required for QuartzJobFactory to get an instance of this registry
        globals.storeServletContext(servletContext);

        registry.performRegistryStartup();
    }

    @Test
    public void testRunJob()
            throws SchedulerException, InterruptedException,
                   ExecutionException, TimeoutException
    {
        Scheduler scheduler = registry.getService(Scheduler.class);
        
        JobDetail job = JobBuilder.newJob(HelloJob.class).build();
        
        job.getJobDataMap().put("username", "John Smith");
        
        Trigger trigger = TriggerBuilder.newTrigger().startNow().build();
        
        QuartzJobFuture<String> future =
                new QuartzJobFuture<String>(scheduler, job.getKey());
        
        scheduler.scheduleJob(job, trigger);
        
        String result = future.get(5, TimeUnit.SECONDS);
        
        String appVersion = registry.getService(SymbolSource.class)
                .valueForSymbol(SymbolConstants.APPLICATION_VERSION);
        
        Assert.assertEquals("username = John Smith, app version = " + appVersion, result);
    }
}
