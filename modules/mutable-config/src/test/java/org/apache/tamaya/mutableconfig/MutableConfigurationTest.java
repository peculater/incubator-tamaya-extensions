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
package org.apache.tamaya.mutableconfig;

import org.apache.tamaya.Configuration;
import org.apache.tamaya.mutableconfig.internal.WritablePropertiesSource;
import org.apache.tamaya.mutableconfig.internal.WritableXmlPropertiesSource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MutableConfiguration}.
 */
public class MutableConfigurationTest {

    @Test
    public void createMutableConfiguration() throws Exception {
        assertThat(MutableConfiguration.create()).isNotNull();
    }

    @Test
    public void createMutableConfiguration1() throws Exception {
        MutableConfiguration cfg = MutableConfiguration.create(Configuration.current());
        assertThat(cfg).isNotNull();
        assertThat(cfg.getChangePropagationPolicy())
                .isEqualTo(ChangePropagationPolicy.MOST_SIGNIFICANT_ONLY_POLICY);
    }

    @Test
    public void createMutableConfiguration2() throws Exception {
        ChangePropagationPolicy policy = ChangePropagationPolicy.getApplySelectiveChangePolicy("blabla");
        MutableConfiguration cfg = MutableConfiguration
                .create(Configuration.current(),
                        policy);
        assertThat(cfg).isNotNull();
        assertThat(cfg.getChangePropagationPolicy()).isEqualTo(policy);
    }

    @Test
    public void createMutableConfiguration3() throws Exception {
        ChangePropagationPolicy policy = ChangePropagationPolicy.getApplySelectiveChangePolicy("gugus");
        MutableConfiguration cfg = MutableConfiguration
                .create(policy);
        assertThat(cfg).isNotNull();
        assertThat(cfg.getChangePropagationPolicy()).isEqualTo(policy);
    }

    /**
     * Test createObject change request.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCreateMutableConfiguration() throws Exception {
        File f = File.createTempFile("ConfigChangeRequest",".properties");
        MutableConfiguration cfg1 = MutableConfiguration.create(
                Configuration.current(),
                ChangePropagationPolicy.ALL_POLICY);
        assertThat(cfg1).isNotNull();
        assertThat(cfg1.getConfigChangeRequest()).isNotNull();
        MutableConfiguration cfg2 = MutableConfiguration.create(
                Configuration.current());
        assertThat(cfg2).isNotNull();
        assertThat(cfg2.getConfigChangeRequest()).isNotNull();
        assertThat(cfg1).isNotEqualTo(cfg2);
        assertThat(cfg1.getConfigChangeRequest()).isNotEqualTo(cfg2.getConfigChangeRequest());
    }

    /**
     * Test null createObject change request.
     *
     * @throws Exception the exception
     */
    @Test(expected=NullPointerException.class)
    public void testNullCreateMutableConfiguration1() throws Exception {
        MutableConfiguration.create(
                (Configuration) null);
    }

    /**
     * Test null createObject change request.
     *
     * @throws Exception the exception
     */
    @Test(expected=NullPointerException.class)
    public void testNullCreateMutableConfiguration2() throws Exception {
        MutableConfiguration.create(
                (ChangePropagationPolicy) null);
    }

    /**
     * Test read write properties with rollback.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testReadWriteProperties_WithCancel() throws IOException {
        WritablePropertiesSource.target.delete();
        MutableConfiguration mutConfig = MutableConfiguration.create(
                Configuration.current()
        );
        mutConfig.put("key1", "value1");
        Map<String,String> cm = new HashMap<>();
        cm.put("key2", "value2");
        cm.put("key3", "value3");
    }

    /**
     * Test read write properties with commit.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testReadWriteProperties_WithCommit() throws IOException {
        WritablePropertiesSource.target.delete();
        MutableConfiguration mutConfig = MutableConfiguration.create(
                Configuration.current()
        );
        mutConfig.put("key1", "value1");
        Map<String,String> cm = new HashMap<>();
        cm.put("key2", "value2");
        cm.put("key3", "value3");
        mutConfig.putAll(cm);
        mutConfig.store();
        assertThat(WritablePropertiesSource.target.exists()).isTrue();
        MutableConfiguration mmutConfig2 = MutableConfiguration.create(
                Configuration.current()
        );
        mmutConfig2.remove("foo");
        mmutConfig2.remove("key3");
        mmutConfig2.put("key1", "value1.2");
        mmutConfig2.put("key4", "value4");
        mmutConfig2.store();
        Properties props = new Properties();
        props.load(WritablePropertiesSource.target.toURL().openStream());
        assertThat(props).hasSize(3);
        assertThat("value1.2").isEqualTo(props.getProperty("key1"));
        assertThat("value2").isEqualTo(props.getProperty("key2"));
        assertThat("value4").isEqualTo(props.getProperty("key4"));
    }

    /**
     * Test read write xml properties with commit.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testReadWriteXmlProperties_WithCommit() throws IOException {
        WritableXmlPropertiesSource.target.delete();
        MutableConfiguration cfg = MutableConfiguration.create(
                Configuration.current(), ChangePropagationPolicy.ALL_POLICY);
        cfg.put("key1", "value1");
        Map<String,String> cm = new HashMap<>();
        cm.put("key2", "value2");
        cm.put("key3", "value3");
        cfg.putAll(cm);
        cfg.store();
        assertThat(WritableXmlPropertiesSource.target.exists()).isTrue();
        MutableConfiguration cfg2 = MutableConfiguration.create(
                Configuration.current());
        assertThat(cfg).isNotEqualTo(cfg2);
        cfg2.remove("foo");
        cfg2.remove("key3");
        cfg2.put("key1", "value1.2");
        cfg2.put("key4", "value4");
        cfg2.store();
        Properties props = new Properties();
        props.loadFromXML( WritableXmlPropertiesSource.target.toURL().openStream());
        assertThat(props).hasSize(3);
        assertThat("value1").isEqualTo(props.getProperty("key1"));
        assertThat("value2").isEqualTo(props.getProperty("key2"));
    }

    /**
     * Test read write xml properties with commit.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testWriteWithNoChangePolicy() throws IOException {
        WritableXmlPropertiesSource.target.delete();
        MutableConfiguration cfg = MutableConfiguration.create(
                Configuration.current(),
                ChangePropagationPolicy.NONE_POLICY);
        cfg.put("key1", "value1");
        Map<String,String> cm = new HashMap<>();
        cm.put("key2", "value2");
        cm.put("key3", "value3");
        cfg.putAll(cm);
        cfg.store();
        assertThat(WritableXmlPropertiesSource.target.exists()).isFalse();
    }

}
