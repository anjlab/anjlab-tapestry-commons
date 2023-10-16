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
package com.anjlab.tapestry5.pages;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.WhitelistAccessOnly;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.internal.services.PageRenderQueue;
import org.apache.tapestry5.internal.structure.Page;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ComponentEventRequestParameters;
import org.apache.tapestry5.services.Request;

@WhitelistAccessOnly
public class PublisherSupport
{
    public static final String STORE_ACTIVE_PAGE_NAME = "storeActivePageName";
    
    @Inject private Request request;
    @Inject private PageRenderQueue pageRenderQueue;

    /**
     * Name of URL parameter and request attribute that may hold value of {@link ComponentEventRequestParameters#getActivePageName()}.
     * 
     * Parameter with this name will be added to all event URLs if this event type has listeners in publisher configuration.
     * 
     * This parameter also will be added into {@link Form#FORM_DATA} of all forms,
     * which will invoke {@link #storeActivePageName(String)} on form submit.
     */
    public static final String PARAMETER_ACTIVE_PAGE = "t:ap";
    
    @OnEvent(STORE_ACTIVE_PAGE_NAME)
    public void storeActivePageName(String activePageName)
    {
        request.setAttribute(PublisherSupport.PARAMETER_ACTIVE_PAGE, activePageName);
    }

    public void addActivePageParameter(Link link, ComponentResources resources)
    {
        Page activePage = pageRenderQueue.getRenderingPage();
        
        if (activePage == null)
        {
            activePage = (Page) resources.getPage();
        }
        
        link.addParameter(PublisherSupport.PARAMETER_ACTIVE_PAGE, activePage.getName());
    }

}
