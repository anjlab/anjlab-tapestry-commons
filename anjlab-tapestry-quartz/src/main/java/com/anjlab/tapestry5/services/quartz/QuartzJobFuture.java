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

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Developed mainly for test purposes.
 *
 * WARNING: There will be a memory leak if job instantiation failed.
 *          Use with care.
 *
 * @author dmitrygusev
 */
public class QuartzJobFuture<T> implements Future<T>, JobListener
{
    private static final Logger logger = LoggerFactory.getLogger(QuartzJobFuture.class);

    private boolean vetoed;
    private boolean executed;
    private boolean cancelled;
    private Object result;

    private final ExecutionMatcher executionMatcher;
    private final Scheduler scheduler;
    private final JobKey jobKey;
    private final Object monitor = new Object();
    private final String listenerId = UUID.randomUUID().toString();
    private final Set<String> matchedExecutionIds = new HashSet<>();

    public QuartzJobFuture(Scheduler scheduler, JobKey jobKey) throws SchedulerException
    {
        this(scheduler, jobKey, new AnyExecutionMatcher());
    }

    public QuartzJobFuture(Scheduler scheduler, JobKey jobKey, String executionIdKey, String executionIdValue) throws SchedulerException
    {
        this(scheduler, jobKey, new KeyValueExecutionMatcher(executionIdKey, executionIdValue));
    }

    public QuartzJobFuture(Scheduler scheduler, JobKey jobKey, ExecutionMatcher executionMatcher) throws SchedulerException
    {
        this.scheduler = scheduler;
        this.jobKey = jobKey;
        this.executionMatcher = executionMatcher;

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
        return "QuatrzJobFuture for " + jobKey + " [" + listenerId + "]";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context)
    {
        if (executionMatcher.matched(context))
        {
            this.matchedExecutionIds.add(context.getFireInstanceId());
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context)
    {
        if (!executionMatcher.matched(context))
        {
            return;
        }

        this.matchedExecutionIds.remove(context.getFireInstanceId());

        synchronized (monitor)
        {
            vetoed = true;
            monitor.notify();
        }

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
        if (!executionMatcher.matched(context))
        {
            return;
        }

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
            for (String executionId : matchedExecutionIds)
            {
                scheduler.interrupt(executionId);
            }

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
            long timeoutMillis = unit.toMillis(timeout);

            long deadline = System.currentTimeMillis() + timeoutMillis;

            while (!isDone())
            {
                monitor.wait(timeoutMillis);

                timeoutMillis = deadline - System.currentTimeMillis();

                if (timeoutMillis <= 0)
                {
                    throw new TimeoutException();
                }
            }
        }
        return (T) result;
    }
}