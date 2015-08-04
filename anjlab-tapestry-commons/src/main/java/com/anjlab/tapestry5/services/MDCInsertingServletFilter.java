package com.anjlab.tapestry5.services;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
