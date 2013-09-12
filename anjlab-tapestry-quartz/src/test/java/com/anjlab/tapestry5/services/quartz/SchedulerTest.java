/**
 * Copyright 2013 AnjLab
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
package com.anjlab.tapestry5.services.quartz;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.services.TapestryModule;
import org.junit.BeforeClass;
import org.junit.Test;
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
