package com.anjlab.tapestry5.services.quartz;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloJob implements org.quartz.Job
{
    private static final Logger logger =
            LoggerFactory.getLogger(HelloJob.class);
    
    private String username;
    
    @Inject @Symbol(SymbolConstants.APPLICATION_VERSION)
    private String version;
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException
    {
        String message =
                String.format("username = %s, app version = %s",
                        username, version);
        
        logger.info(message);
        
        context.setResult(message);
    }
    
    public void setUsername(String username)
    {
        this.username = username;
    }
}