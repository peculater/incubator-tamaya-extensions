/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tamaya.inject;

import org.apache.tamaya.spi.ServiceContextManager;

/**
 * Singleton accessor class for accessing {@link ConfigurationInjector} instances.
 * @deprecated Use {@link ConfigurationInjector}
 */
@Deprecated
public final class ConfigurationInjection {

    /**
     * Singleton constructor.
     */
    private ConfigurationInjection() {
    }

    /**
     * Get the current injector instance, using the default classloader.
     *
     * @return the current injector, not null.
     */
    public static ConfigurationInjector getConfigurationInjector() {
        return ServiceContextManager.getServiceContext(ServiceContextManager.getDefaultClassLoader())
                .getService(ConfigurationInjector.class);
    }

    /**
     * Get the current injector instance, using the given target classloader.
     *
     * @param classLoader the classloader, not null.
     * @return the current injector, not null.
     */
    public static ConfigurationInjector getConfigurationInjector(ClassLoader classLoader) {
        return ServiceContextManager.getServiceContext(classLoader)
                .getService(ConfigurationInjector.class);
    }
}
