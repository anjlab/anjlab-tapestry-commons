package com.anjlab.tapestry5.services;


public interface InjectionHelper
{

    /**
     * 
     * @param target
     * @throws NoSuchMethodException
     */
    void invokePostInjection(Object target) throws NoSuchMethodException;

    void injectFields(Object target);

    void inject(Object target);

}
