package com.anjlab.tapestry5.services.quartz;

import javax.servlet.ServletContext;

import org.apache.tapestry5.TapestryFilter;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ApplicationGlobals;
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
    private ApplicationGlobals applicationGlobals;
    
    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
            throws SchedulerException
    {
        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends Job> jobClass = jobDetail.getJobClass();
        
        ServletContext servletContext = applicationGlobals.getServletContext();
        
        Registry registry = (Registry)
                servletContext.getAttribute(TapestryFilter.REGISTRY_CONTEXT_NAME);
        
        Job job = registry.autobuild(jobClass);
        
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAll(scheduler.getContext());
        jobDataMap.putAll(bundle.getJobDetail().getJobDataMap());
        jobDataMap.putAll(bundle.getTrigger().getJobDataMap());
        
        setBeanProps(job, jobDataMap);
        
        return job;
    }

}
