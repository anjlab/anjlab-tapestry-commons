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
package com.anjlab.tapestry5.services.events.internal;

import java.util.Collection;

import org.apache.tapestry5.plastic.PlasticMethod;


public interface PublisherConfiguration
{
    void addListener(String eventType, Object listener);
    void removeListener(String eventType, Object listener);
    
    void addTrigger(String eventType, PlasticMethod trigger);
    
    void addEventHandler(String value, PlasticMethod handler);
    
    void clear();
    
    Iterable<String> eventTypes();
    
    Iterable<?> listeners(String eventType);
    Iterable<?> triggers(String eventType);
    Iterable<?> handlers(String eventType);
    
    boolean isManagedEvent(String eventType);
    
    void addManagedEvents(Collection<String> managedEvents);
}
