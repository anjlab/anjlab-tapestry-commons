package com.anjlab.tapestry5.services.quartz;

import org.quartz.JobExecutionContext;

public interface ExecutionMatcher
{
    boolean matched(JobExecutionContext context);
}
