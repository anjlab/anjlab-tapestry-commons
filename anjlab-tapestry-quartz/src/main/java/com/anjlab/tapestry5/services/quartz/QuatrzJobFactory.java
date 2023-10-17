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

import org.apache.tapestry5.commons.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

public class QuatrzJobFactory extends PropertySettingJobFactory implements JobFactory
{
    @Inject
    private ObjectLocator objectLocator;
    
    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
            throws SchedulerException
    {
        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends Job> jobClass = jobDetail.getJobClass();
        
        Job job;
        
        try
        {
            job = objectLocator.autobuild(jobClass);
        }
        catch (Exception e)
        {
            SchedulerException se = new SchedulerException(
                    "Problem instantiating class '"
                            + jobDetail.getJobClass().getName() + "'", e);
            throw se;
        }
        
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAll(scheduler.getContext());
        jobDataMap.putAll(bundle.getJobDetail().getJobDataMap());
        jobDataMap.putAll(bundle.getTrigger().getJobDataMap());
        
        setBeanProps(job, jobDataMap);
        
        return job;
    }

}
