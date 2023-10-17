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
package com.anjlab.tapestry5.services.liquibase;

import java.io.Closeable;
import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.tapestry5.func.F;
import org.apache.tapestry5.func.Predicate;
import org.apache.tapestry5.commons.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.annotations.Advise;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.jpa.EntityManagerSource;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Wrapped;
import org.slf4j.Logger;

public class AutoConfigureLiquibaseDataSourceFromHibernateJPAModule
{
    // If JNDI name of the data source starts with "jdbc/" LiquibaseServletListener tries to do a
    // lookup from "java:comp/env" context. This context is read-only, so we cannot bind to it.
    // That's why we're binding directly to the scope of InitialContext.
    private static final String LIQUIBASE_DATASOURCE_JNDI_NAME = "liquibase-datasource";

    public static final String LIQUIBASE_PERSISTENCE_UNIT_NAME = "liquibase.datasource.persistence-unit-name";

    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        configuration.override(LiquibaseModule.LIQUIBASE_DATA_SOURCE, LIQUIBASE_DATASOURCE_JNDI_NAME);

        configuration.add(LIQUIBASE_PERSISTENCE_UNIT_NAME, "");
    }

    @Advise(serviceInterface = LiquibaseInitializer.class)
    public static void dataSourceForLiquibase(
            final MethodAdviceReceiver receiver,
            final EntityManagerSource entityManagerSource,
            @Inject @Symbol(LIQUIBASE_PERSISTENCE_UNIT_NAME)
            final String persistenceUnitName,
            final Logger logger)
    {
        receiver.adviseAllMethods(new MethodAdvice()
        {
            @Override
            public void advise(MethodInvocation invocation)
            {
                InitialContext ic = null;
                DataSource dataSource = null;
                try
                {
                    // At first we tried to use the same instance of a data source that's used by
                    // the EntityManager, but this didn't work when "hibernate.hbm2ddl.auto"
                    // property set to "validate".
                    //
                    // This is because Liquibase was taking value from Hibernate and Hibernate
                    // in turn failed to start because of schema-validation errors.
                    //
                    // Instead Liquibase should start first and fix the database schema that is then
                    // will be used and validated by Hibernate.
                    //
                    // That's why we're creating separate data source here, though using the same
                    // values taken from `persistence.xml`

                    PersistenceUnitInfo persistenceUnitInfo =
                            F.flow(entityManagerSource.getPersistenceUnitInfos())
                                    .filter(new Predicate<PersistenceUnitInfo>()
                    {
                        @Override
                        public boolean accept(PersistenceUnitInfo unitInfo)
                        {
                            return unitInfo.getPersistenceUnitName().equals(persistenceUnitName);
                        }
                    }).first();

                    if (persistenceUnitInfo == null)
                    {
                        // Taking first persistence unit by default
                        persistenceUnitInfo = entityManagerSource.getPersistenceUnitInfos().get(0);
                    }

                    String connectionProviderClassName =
                            (String) persistenceUnitInfo.getProperties()
                                    .get(AvailableSettings.CONNECTION_PROVIDER);

                    Class<?> connectionProviderClass = Class.forName(connectionProviderClassName);
                    Object connectionProvider = connectionProviderClass.newInstance();
                    ((Configurable) connectionProvider).configure(persistenceUnitInfo.getProperties());
                    dataSource = ((Wrapped) connectionProvider).unwrap(DataSource.class);

                    ic = new InitialContext();

                    ic.bind(LIQUIBASE_DATASOURCE_JNDI_NAME, dataSource);

                    invocation.proceed();
                }
                catch (Throwable e)
                {
                    logger.error("Error binding liquibase datasource", e);

                    throw new RuntimeException(e);
                }
                finally
                {
                    if (ic != null)
                    {
                        try
                        {
                            ic.unbind(LIQUIBASE_DATASOURCE_JNDI_NAME);
                        }
                        catch (NamingException e)
                        {
                            logger.error("Error unbinding liquibase datasource", e);
                        }

                        try
                        {
                            ic.close();
                        }
                        catch (NamingException e)
                        {
                            logger.error("Error closing InitialContext", e);
                        }
                    }

                    if (dataSource instanceof Closeable)
                    {
                        try
                        {
                            ((Closeable) dataSource).close();
                        }
                        catch (IOException e)
                        {
                            logger.error("Error closing liquibase datasource", e);
                        }
                    }
                }
            }
        });
    }

}
