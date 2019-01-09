/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tamaya.microprofile;

import org.apache.tamaya.Configuration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by atsticks on 24.03.17.
 */
public class MicroprofileConfigProviderTest {

    @Test
    public void testDefaultConfigAccess(){
        Config config = ConfigProvider.getConfig();
        assertThat(config).isNotNull();
        Iterable<String> names = config.getPropertyNames();
        assertThat(names).isNotNull();
        int count = 0;
        for(String name:names){
            count++;
            System.out.println(count + ": " +name);
        }
        assertThat(Configuration.current().getProperties().size() <= count).isTrue();
    }

    @Test
    public void testClassloaderAccess(){
        Config config = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());
        assertThat(config).isNotNull();
        Iterable<String> names = config.getPropertyNames();
        assertThat(names).isNotNull();
        int count = 0;
        for(String name:names){
            count++;
        }
        assertThat(count > 0).isTrue();
    }

}
