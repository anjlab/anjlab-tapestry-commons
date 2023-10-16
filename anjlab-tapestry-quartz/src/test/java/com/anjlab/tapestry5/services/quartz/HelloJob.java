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