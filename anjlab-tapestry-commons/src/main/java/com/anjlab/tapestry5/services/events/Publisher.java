/**
 * Copyright 2014 AnjLab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.tapestry5.services.events;

import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.runtime.Component;

public interface Publisher
{
    /**
     * 
     * @param eventType
     * @param contextValues
     * @param callback
     * @return
     * 
     * @see ComponentResources#triggerEvent(String, Object[], ComponentEventCallback)
     */
    boolean triggerEvent(String eventType, Object[] contextValues, ComponentEventCallback<?> callback);

    /**
     * 
     * @param eventType
     * @param context
     * @param callback
     * @return
     * 
     * @see ComponentResources#triggerContextEvent(String, EventContext, ComponentEventCallback)
     */
    boolean triggerContextEvent(String eventType, EventContext context, ComponentEventCallback<?> callback);

    void subscribe(String eventType, Object listener);
    
    boolean isActivePage(Component page);
}
