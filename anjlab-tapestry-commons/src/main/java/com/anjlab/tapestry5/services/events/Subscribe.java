package com.anjlab.tapestry5.services.events;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.tapestry5.ioc.annotations.AnnotationUseContext.COMPONENT;
import static org.apache.tapestry5.ioc.annotations.AnnotationUseContext.PAGE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.tapestry5.ioc.annotations.UseWith;

/**
 * A marker annotation applied to a page or component subscribes this page/component to specified
 * published events.
 * This has the same effect as invoking {@link Publisher#subscribe(String, Object)} on target
 * component.
 */
@Target(TYPE)
@Documented
@Retention(RUNTIME)
@Inherited
@UseWith({ COMPONENT, PAGE })
public @interface Subscribe
{
    /**
     * List of event types this page / component will be subscribed to.
     */
    String[] value();
}
