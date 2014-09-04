package com.anjlab.tapestry5.services.events.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.internal.transform.OnEventWorker;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.internal.util.TapestryException;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.ConstructorCallback;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
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
        final List<String> eventTypes = new ArrayList<String>();

        findEventTypesOnClass(plasticClass, eventTypes);

        findEventTypesOnMethods(plasticClass, eventTypes);

        if (eventTypes.isEmpty())
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
                        for (String eventType : eventTypes)
                        {
                            publisher.subscribe(eventType, instance);
                        }
                    }
                });
            }
        };

        plasticClass.onConstruct(subscribeOnPageLoaded);
    }

    private void findEventTypesOnMethods(PlasticClass plasticClass, final List<String> eventTypes)
    {
        for (final PlasticMethod method : plasticClass.getMethodsWithAnnotation(Subscribe.class))
        {
            assertNoEventTypeSpecified(method);

            String eventType = extractEventType(method.getDescription().methodName, method.getAnnotation(OnEvent.class));

            eventTypes.add(eventType);
        }
    }

    /**
     * Copied from {@link OnEventWorker}
     */
    private String extractEventType(String methodName, OnEvent annotation)
    {
        if (annotation != null)
            return annotation.value();

        int fromx = methodName.indexOf("From");

        // The first two characters are always "on" as in "onActionFromFoo".
        return fromx == -1 ? methodName.substring(2) : methodName.substring(2, fromx);
    }

    private void assertNoEventTypeSpecified(PlasticMethod method)
    {
        Subscribe annotation = method.getAnnotation(Subscribe.class);

        if (!hasOnlyDefaultValue(annotation))
        {
            throw new TapestryException(String.format(
                    "@Subscribe when put on methods shouldn't have any values (%s). Use @OnEvent to specify event name:  %s",
                    Arrays.asList(annotation.value()).toString(), method.getMethodIdentifier()),
                    null);
        }
    }

    private boolean hasOnlyDefaultValue(Subscribe annotation)
    {
        String[] eventTypes = annotation.value();

        // @Subscribe's default value is an [""]

        return eventTypes.length == 1 && "".equals(eventTypes[0]);
    }

    private void findEventTypesOnClass(final PlasticClass plasticClass, final List<String> eventTypes)
    {
        if (!plasticClass.hasAnnotation(Subscribe.class))
        {
            return;
        }

        Subscribe annotation = plasticClass.getAnnotation(Subscribe.class);

        if (hasOnlyDefaultValue(annotation))
        {
            throw new TapestryException(String.format(
                    "@Subscribe annotaion when put on classes should have eventType(s) specified: %s",
                    plasticClass.getClassName()), null);
        }

        for (String eventType : annotation.value())
        {
            eventTypes.add(eventType);
        }
    }
}