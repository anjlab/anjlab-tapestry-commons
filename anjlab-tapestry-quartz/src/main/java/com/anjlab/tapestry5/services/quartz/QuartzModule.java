/**
 * Copyright 2014 AnjLab
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
package com.anjlab.tapestry5.services.quartz;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.tapestry5.commons.MappedConfiguration;
import org.apache.tapestry5.commons.Resource;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;

public class QuartzModule
{
    public static final String QUARTZ_PROPERTIES = "quartz.properties";
    public static final String WAIT_FOR_JOBS_TO_COMPLETE = "org.quartz.scheduler.waitForJobsToComplete";
    public static final String START_SCHEDULERS = "org.quartz.scheduler.start";

    public static void bind(ServiceBinder binder)
    {
        binder.bind(JobFactory.class, QuatrzJobFactory.class);
    }
    
    public static void contributeFactoryDefaults(MappedConfiguration<String, Object> configuration)
    {
        configuration.add(WAIT_FOR_JOBS_TO_COMPLETE, "false");
        configuration.add(START_SCHEDULERS, "true");
    }
    
    public static void contributeSchedulerFactory(MappedConfiguration<String, Object> configuration)
    {
        configuration.add("org.quartz.scheduler.skipUpdateCheck", "true");
        configuration.add("org.quartz.scheduler.instanceName", "TapestryQuartz");
        configuration.add("org.quartz.threadPool.threadCount", "3");
        configuration.add("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
    }
    
    public static SchedulerFactory buildSchedulerFactory(
            Map<String, Object> configuration) throws SchedulerException, IOException
    {
        StdSchedulerFactory sf = new StdSchedulerFactory();
        
        if (configuration.containsKey(QUARTZ_PROPERTIES))
        {
            Object properties = configuration.get(QUARTZ_PROPERTIES);
            
            if (properties instanceof String)
            {
                sf.initialize((String) properties);
            }
            else if (properties instanceof Properties)
            {
                sf.initialize((Properties) properties);
            }
            else if (properties instanceof InputStream)
            {
                sf.initialize((InputStream) properties);
            }
            else if (properties instanceof Resource)
            {
                sf.initialize(((Resource) properties).openStream());
            }
            else
            {
                throw new RuntimeException("Unsupported type of " + QUARTZ_PROPERTIES);
            }
        }
        else
        {
            Properties properties = new Properties();
            
            properties.putAll(configuration);
            
            sf.initialize(properties);
        }
        
        return sf;
    }
    
    public static Scheduler buildScheduler(SchedulerFactory schedulerFactory, JobFactory jobFactory)
            throws SchedulerException
    {
        Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.setJobFactory(jobFactory);
        return scheduler;
    }
    
    @Startup
    public static void startup(
                        final Logger logger,
                        final SchedulerFactory schedulerFactory,
                        final PerthreadManager perthreadManager,
                        RegistryShutdownHub shutdownHub,
                        @Inject @Symbol(WAIT_FOR_JOBS_TO_COMPLETE)
                        final boolean waitForJobsToComplete,
                        @Inject @Symbol(START_SCHEDULERS)
                        final boolean startSchedulers) throws SchedulerException
    {
        final JobListenerSupport cleanupThread = new JobListenerSupport()
        {
            @Override
            public String getName()
            {
                return "PerthreadManager.cleanup()";
            }
            
            @Override
            public void jobWasExecuted(JobExecutionContext context,
                    JobExecutionException jobException)
            {
                perthreadManager.cleanup();
            }
        };
        
        Collection<Scheduler> allSchedulers = new ArrayList<Scheduler>();
        
        allSchedulers.addAll(schedulerFactory.getAllSchedulers());
        allSchedulers.add(schedulerFactory.getScheduler());
        
        for (Scheduler scheduler : allSchedulers)
        {
            if (!scheduler.isStarted())
            {
                scheduler.getListenerManager().addJobListener(cleanupThread);

                if (startSchedulers)
                {
                    scheduler.start();
                }
            }
        }
        
        shutdownHub.addRegistryWillShutdownListener(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    for (Scheduler scheduler : schedulerFactory.getAllSchedulers())
                    {
                        scheduler.shutdown(waitForJobsToComplete);
                    }
                }
                catch (SchedulerException e)
                {
                    logger.error("Error shutting down scheduler", e);
                }
            }
        });
    }
}
