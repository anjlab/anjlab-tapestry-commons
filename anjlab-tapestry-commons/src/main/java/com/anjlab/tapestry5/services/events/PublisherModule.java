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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.tapestry5.ComponentAction;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.corelib.internal.FormSupportImpl;
import org.apache.tapestry5.internal.services.PageRenderQueue;
import org.apache.tapestry5.internal.structure.Page;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Advise;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Decorate;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.apache.tapestry5.runtime.Component;
import org.apache.tapestry5.services.ComponentEventRequestParameters;
import org.apache.tapestry5.services.ComponentSource;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.FormSupport;
import org.apache.tapestry5.services.LibraryMapping;
import org.apache.tapestry5.services.LinkCreationHub;
import org.apache.tapestry5.services.LinkCreationListener2;
import org.apache.tapestry5.services.PageRenderRequestParameters;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;

import com.anjlab.tapestry5.pages.PublisherSupport;
import com.anjlab.tapestry5.services.events.internal.PublisherConfiguration;
import com.anjlab.tapestry5.services.events.internal.PublisherTriggersIntrospector;

public class PublisherModule
{
    private static final class StoreParameterActivePageToRequest implements
            ComponentAction<Component>, Serializable
    {
        private static final long serialVersionUID = -1L;
        
        private final String activePageName;
        
        private StoreParameterActivePageToRequest(String activePageName)
        {
            this.activePageName = activePageName;
        }
        
        @Override
        public void execute(Component component)
        {
            component.getComponentResources().triggerEvent(
                    PublisherSupport.STORE_ACTIVE_PAGE_NAME,
                    new Object[] { activePageName },
                    null);
        }
    }

    private static final class FormSupportAdvice implements MethodAdvice
    {
        private final ComponentSource componentSource;
        private final PageRenderQueue pageRenderQueue;
        private final Field formSupportResourcesField;

        private FormSupportAdvice(ComponentSource componentSource,
                PageRenderQueue pageRenderQueue)
        {
            this.componentSource = componentSource;
            this.pageRenderQueue = pageRenderQueue;
            try
            {
                formSupportResourcesField = FormSupportImpl.class.getDeclaredField("resources");
                formSupportResourcesField.setAccessible(true);
            }
            catch (Exception e)
            {
                throw createTapestryInternalsChangedException(e);
            }
        }

        private RuntimeException createTapestryInternalsChangedException(Exception e)
        {
            return new RuntimeException("Tapestry internals changed", e);
        }

        private ComponentResources getComponentResources(FormSupport formSupport)
        {
            try
            {
                return (ComponentResources) formSupportResourcesField.get(formSupport);
            }
            catch (Exception e)
            {
                throw createTapestryInternalsChangedException(e);
            }
        }

        @Override
        public void advise(MethodInvocation invocation)
        {
            invocation.proceed();
            
            Class<?> type = (Class<?>) invocation.getParameter(0);
            
            if (type.equals(FormSupport.class))
            {
                FormSupport formSupport = (FormSupport) invocation.getParameter(1);
                
                if (formSupport.getClientId() == null)
                {
                    //  Submit
                    return;
                }
                
                Page activePage = pageRenderQueue.getRenderingPage();
                
                if (activePage == null)
                {
                    activePage = (Page) getComponentResources(formSupport).getPage();
                }
                
                final String activePageName = activePage.getName();
                
                Component component = componentSource.getPage(PublisherSupport.class);
                
                formSupport.store(component, new StoreParameterActivePageToRequest(activePageName));
            }
        }
    }

    public static void bind(ServiceBinder binder)
    {
        binder.bind(Publisher.class);
        binder.bind(PublisherConfiguration.class);
    }
    
    public static void contributeComponentClassResolver(Configuration<LibraryMapping> configuration)
    {
        configuration.add(new LibraryMapping("anjlab", "com.anjlab.tapestry5"));
    }
    
    @Contribute(ComponentClassTransformWorker2.class)
    public void contributeComponentClassTransformWorker2(
          OrderedConfiguration<ComponentClassTransformWorker2> configuration,
          PublisherConfiguration publisherConfiguration)
    {
       configuration.add("anjlab-publisher", new PublisherTriggersIntrospector(publisherConfiguration)); 
    }
    
    @Advise(serviceInterface=Environment.class)
    public void adviseEnvironment(MethodAdviceReceiver receiver,
            final ComponentSource componentSource,
            final Request request,
            final PageRenderQueue pageRenderQueue) throws NoSuchMethodException, SecurityException
    {
        @SuppressWarnings("unchecked")
        Method pushMethod = receiver.getInterface().getDeclaredMethod("push", Class.class, Object.class);
        
        receiver.adviseMethod(pushMethod,
                new FormSupportAdvice(componentSource, pageRenderQueue));
    }
    
    @Decorate(serviceInterface=LinkCreationHub.class)
    public LinkCreationHub addLinkCreationListener(LinkCreationHub linkCreationHub,
            final PublisherConfiguration publisherConfiguration)
    {
        linkCreationHub.addListener(new LinkCreationListener2()
        {
            @Override
            public void createdPageRenderLink(Link link, PageRenderRequestParameters parameters) {}
            
            @Override
            public void createdComponentEventLink(Link link, ComponentEventRequestParameters parameters)
            {
                if (publisherConfiguration.isManagedEvent(parameters.getEventType()))
                {
                    addCurrentPageParameterForPublisher(link, parameters);
                }
            }

            private void addCurrentPageParameterForPublisher(Link link,
                    ComponentEventRequestParameters parameters)
            {
                link.addParameter(PublisherSupport.PARAMETER_ACTIVE_PAGE, parameters.getActivePageName());
            }
        });

        return linkCreationHub;
    }
}
