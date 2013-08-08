package com.anjlab.tapestry5.services;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.tapestry5.ioc.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathUtils
{
    private static final Logger logger = LoggerFactory.getLogger(ClasspathUtils.class);
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void autobindServices(ServiceBinder binder, Package interfacesPackage) throws ClassNotFoundException
    {
        List<Class<?>> interfaces = getClassesForPackage(interfacesPackage.getName());
        for (Class intf : interfaces) {
            String className = interfacesPackage.getName() + ".impl." + intf.getSimpleName() + "Impl";
            try {
                Class impl = Class.forName(className);
                binder.bind(intf, impl);
            }
            catch (ClassNotFoundException e) {
                logger.warn("Class not found during autobinding: {}", className);
            }
        }
    }
    
    public static List<Class<?>> getClassesForPackage(String packageName)
            throws ClassNotFoundException
    {
        // This will hold a list of directories matching the packageName.
        // There may be more than one if a package is split over multiple
        // jars/paths
        List<Class<?>> classes = new ArrayList<Class<?>>();
        List<File> directories = new ArrayList<File>();
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            // Ask for all resources for the path
            Enumeration<URL> resources = cld.getResources(packageName.replace('.', '/'));
            while (resources.hasMoreElements()) {
                URL res = resources.nextElement();
                if (res.getProtocol().equalsIgnoreCase("jar")) {
                    JarURLConnection conn = (JarURLConnection) res.openConnection();
                    JarFile jar = conn.getJarFile();
                    for (JarEntry e : Collections.list(jar.entries())) {

                        if (e.getName().startsWith(packageName.replace('.', '/')) && e.getName().endsWith(".class")
                                && !e.getName().contains("$")) {
                            String className = e.getName().replace("/", ".").substring(0, e.getName().length() - ".class".length());
                            classes.add(Class.forName(className));
                        }
                    }
                } else {
                    directories.add(new File(URLDecoder.decode(res.getPath(), "UTF-8")));
                }
            }
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(packageName + " does not appear to be "
                    + "a valid package (Null pointer exception)");
        } catch (UnsupportedEncodingException encex) {
            throw new ClassNotFoundException(packageName + " does not appear to be "
                    + "a valid package (Unsupported encoding)");
        } catch (IOException ioex) {
            throw new ClassNotFoundException("IOException was thrown when trying " + "to get all resources for "
                    + packageName);
        }

        // For every directory identified capture all the .class files
        for (File directory : directories) {
            if (directory.exists()) {
                // Get the list of the files contained in the package
                String[] files = directory.list();
                for (String file : files) {
                    // we are only interested in .class files
                    if (file.endsWith(".class")) {
                        // removes the .class extension
                        classes.add(Class.forName(packageName + '.' + file.substring(0, file.length() - 6)));
                    }
                }
            } else {
                throw new ClassNotFoundException(packageName + " (" + directory.getPath()
                        + ") does not appear to be a valid package");
            }
        }
        return classes;
    }
}
