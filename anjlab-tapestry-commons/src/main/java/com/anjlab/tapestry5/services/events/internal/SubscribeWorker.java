package com.anjlab.tapestry5.services.events.internal;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.ConstructorCallback;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.runtime.Component;
import org.apache.tapestry5.runtime.PageLifecycleCallbackHub;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

import com.anjlab.tapestry5.services.events.Publisher;
import com.anjlab.tapestry5.services.events.Subscribe;

public class SubscribeWorker implements ComponentClassTransformWorker2
{
    @Inject
    private Publisher publisher;

    @Override
    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        final Subscribe annotation = plasticClass.getAnnotation(Subscribe.class);

        if (annotation == null)
        {
            return;
        }

        ConstructorCallback subscribeOnPageLoaded = new ConstructorCallback()
        {
            @Override
            public void onConstruct(final Object instance, InstanceContext context)
            {
                final PageLifecycleCallbackHub pageLifecycleCallbackHub =
                        instance instanceof PageLifecycleCallbackHub
                                ? (PageLifecycleCallbackHub) instance
                                : instance instanceof Component
                                        ? ((Component) instance).getComponentResources().getPageLifecycleCallbackHub()
                                        : null;

                // The annotation should be applied to page / component classes only for which we
                // know how to obtain the PageLifecycleCallbackHub instance
                assert pageLifecycleCallbackHub != null;

                pageLifecycleCallbackHub.addPageLoadedCallback(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (String eventType : annotation.value())
                        {
                            publisher.subscribe(eventType, instance);
                        }
                    }
                });
            }
        };

        plasticClass.onConstruct(subscribeOnPageLoaded);
    }
}