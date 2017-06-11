/**
 * Copyright 2017 AnjLab
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
package com.anjlab.tapestry5.config.services;

/**
 * Logic for validation of {@link ConfigHelper} instance.
 * <p>
 * Contribute instance of this interface into {@link ConfigHelperInitializer}:
 * <pre>
 * public void contributeConfigHelperInitializer(OrderedConfiguration&lt;ConfigHelperValidator&gt; configuration)
 * {
 *     configuration.addInstance("CustomConfigValidator", CustomConfigValidator.class);
 * }
 * </pre>
 */
public interface ConfigHelperValidator
{
    /**
     * Validate given {@link ConfigHelper} and throw {@link RuntimeException} in case of validation failure.
     * <p>
     * Throwing an exception will fail application startup.
     * @param configHelper {@link ConfigHelper} to validate.
     */
    void validate(ConfigHelper configHelper) throws RuntimeException;
}
