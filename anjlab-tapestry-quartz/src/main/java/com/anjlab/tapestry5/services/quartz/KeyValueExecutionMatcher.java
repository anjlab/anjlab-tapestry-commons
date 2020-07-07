package com.anjlab.tapestry5.services.quartz;

import org.quartz.JobExecutionContext;

public class KeyValueExecutionMatcher implements ExecutionMatcher
{
    private final String keyName;
    private final String value;

    public KeyValueExecutionMatcher(String keyName, String value)
    {
        this.keyName = keyName;
        this.value = value;
    }

    @Override
    public boolean matched(JobExecutionContext context)
    {
        return value.equals(context.getTrigger().getJobDataMap().get(keyName));
    }
}