/**
 * Copyright 2015 AnjLab
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
package com.anjlab.tapestry5.services;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MDCInsertingServletFilter extends ch.qos.logback.classic.helpers.MDCInsertingServletFilter
{
    private static final Logger logger = LoggerFactory.getLogger(MDCInsertingServletFilter.class);
    
    public static final String REQUEST_ID = "req.requestId";
    public static final String REQUEST_SESSION_ID = "req.sessionId";

    private final AtomicLong requestCounter = new AtomicLong(0);
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        
        insertMDC(httpServletRequest);
        
        try
        {
            logger.info("{} {}{}",
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI(),
                    (httpServletRequest.getQueryString() == null)
                        ? ""
                        : "?" + httpServletRequest.getQueryString());
            
            super.doFilter(request, response, chain);
        }
        finally
        {
            cleanupMDC();
        }
    }

    private void cleanupMDC()
    {
        MDC.remove(REQUEST_ID);
        MDC.remove(REQUEST_SESSION_ID);
    }

    protected void insertMDC(HttpServletRequest httpServletRequest)
    {
        HttpSession session = httpServletRequest.getSession(false);
        
        String sessionId = "-";
        if (session != null)
        {
            sessionId = session.getId();
        }
        
        MDC.put(REQUEST_ID, String.valueOf(requestCounter.incrementAndGet()));
        MDC.put(REQUEST_SESSION_ID, sessionId);
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

}
