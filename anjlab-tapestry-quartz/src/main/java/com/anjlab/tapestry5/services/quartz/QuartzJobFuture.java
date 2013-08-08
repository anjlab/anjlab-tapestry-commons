package com.anjlab.tapestry5.services.quartz;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Developed mainly for test purposes.
 * 
 * @author dmitrygusev
 *
 * @param <T>
 */
public class QuartzJobFuture<T> implements Future<T>, JobListener
{
    private static final Logger logger = LoggerFactory.getLogger(QuartzJobFuture.class);
    
    private volatile boolean vetoed;
    private volatile boolean executed;
    
    private boolean cancelled;
    
    private Object result;
    
    private final Scheduler scheduler;
    private final JobKey jobKey;
    
    private final Object monitor = new Object();
    
    public QuartzJobFuture(Scheduler scheduler, JobKey jobKey) throws SchedulerException
    {
        this.scheduler = scheduler;
        this.jobKey = jobKey;
        
        scheduler.getListenerManager().addJobListener(this, new Matcher<JobKey>()
        {
            private static final long serialVersionUID = 1L;
            
            public boolean isMatch(JobKey key)
            {
                return key.equals(QuartzJobFuture.this.jobKey);
            }
        });
    }

    @Override
    public String getName()
    {
        return "QuatrzJobFuture for " + jobKey;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context)
    {
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context)
    {
        vetoed = true;
        
        removeListener();
    }

    private void removeListener()
    {
        try
        {
            scheduler.getListenerManager().removeJobListener(this.getName());
        }
        catch (SchedulerException e)
        {
            logger.error("Error removing job listener " + jobKey, e);
        }
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException)
    {
        removeListener();
        
        synchronized (monitor)
        {
            result = context.getResult();
            executed = true;
            
            monitor.notify();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (cancelled)
        {
            return true;
        }
        
        if (!mayInterruptIfRunning && isRunning())
        {
            return false;
        }
        
        try
        {
            scheduler.interrupt(jobKey);
            
            cancelled = true;
        }
        catch (SchedulerException e)
        {
            logger.error("Error interrupting job " + jobKey, e);
        }
        
        return cancelled;
    }

    private boolean isRunning()
    {
        try
        {
            for (JobExecutionContext context : scheduler.getCurrentlyExecutingJobs())
            {
                if (context.getJobDetail().getKey().equals(jobKey))
                {
                    return true;
                }
            }
        }
        catch (SchedulerException e)
        {
            logger.error("Error determining if job is running: " + jobKey, e);
        }
        
        return false;
    }

    @Override
    public boolean isCancelled()
    {
        return vetoed || cancelled;
    }

    @Override
    public boolean isDone()
    {
        return executed || vetoed || !isExists();
    }

    private boolean isExists()
    {
        try
        {
            return scheduler.checkExists(jobKey);
        }
        catch (SchedulerException e)
        {
            logger.error("Error checking if job exists: " + jobKey, e);
        }
        
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() throws InterruptedException, ExecutionException
    {
        synchronized (monitor)
        {
            while (!isDone())
            {
                monitor.wait();
            }
        }
        return (T) result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException
    {
        synchronized (monitor)
        {
            while (!isDone())
            {
                monitor.wait(unit.toMillis(timeout));
            }
        }
        return (T) result;
    }
    
}