/**
 * Copyright 2017 AnjLab
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