package com.anjlab.tapestry5.services.quartz;

import org.quartz.JobExecutionContext;

public class AnyExecutionMatcher implements ExecutionMatcher
{
    @Override
    public boolean matched(JobExecutionContext context)
    {
        return true;
    }
}