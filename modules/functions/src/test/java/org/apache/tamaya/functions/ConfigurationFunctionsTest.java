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
package org.apache.tamaya.functions;

import org.apache.tamaya.Configuration;
import org.apache.tamaya.spi.ConfigurationBuilder;
import org.apache.tamaya.spisupport.propertysource.EnvironmentPropertySource;
import org.apache.tamaya.spisupport.propertysource.SystemPropertySource;
import org.junit.Test;

import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Anatole on 01.10.2015.
 */
public class ConfigurationFunctionsTest {

    @Test
    public void testFilter() throws Exception {
// TODO implement test
    }

    @Test
    public void testMap() throws Exception {
// TODO implement test
    }

    @Test
    public void testSection() throws Exception {
        // Tests with/without stripping of keys:
        // See https://issues.apache.org/jira/browse/TAMAYA-235
        testSection(false);
        testSection(true);
    }

    @Test
    public void testSection1() throws Exception {
// TODO implement test
    }

    @Test
    public void testIsKeyInSection() throws Exception {
// TODO implement test
    }

    @Test
    public void testIsKeyInSections() throws Exception {
// TODO implement test
    }

    @Test
    public void testSections() throws Exception {
// TODO implement test
    }

    @Test
    public void testTransitiveSections() throws Exception {
// TODO implement test
    }

    @Test
    public void testSections1() throws Exception {
// TODO implement test
    }

    @Test
    public void testTransitiveSections1() throws Exception {
// TODO implement test
    }

    @Test
    public void testSectionsRecursive() throws Exception {
// TODO implement test
    }

    @Test
    public void testCombine() throws Exception {
// TODO implement test
    }

    @Test
    public void testPropertySourceFrom() throws Exception {
// TODO implement test
    }

    @Test
    public void testSectionRecursive() throws Exception {
// TODO implement test
    }

    @Test
    public void testJsonInfo() throws Exception {
// TODO implement test
    }

    @Test
    public void testJsonInfo1() throws Exception {
// TODO implement test
    }

    @Test
    public void testXmlInfo() throws Exception {
// TODO implement test
    }

    @Test
    public void testXmlInfo1() throws Exception {
// TODO implement test
    }

    @Test
    public void testTextInfo() throws Exception {
// TODO implement test
    }

    @Test
    public void testTextInfo1() throws Exception {
// TODO implement test
    }

    @Test
    public void testAddItems() throws Exception {
// TODO implement test
    }

    @Test
    public void testEmptyConfiguration() throws Exception {
        Configuration ps = ConfigurationFunctions.emptyConfiguration();
        assertThat(ps).isNotNull();
        assertThat(ps.getProperties()).isNotNull();
        assertThat(ps.getProperties().isEmpty()).isTrue();
    }


    private void testSection(boolean stripKeys){
        ConfigurationBuilder b = Configuration.createConfigurationBuilder();
        b.addPropertySources(new EnvironmentPropertySource(), new SystemPropertySource());
        Configuration cfg = b.build().map(
                ConfigurationFunctions.section("java.", stripKeys));
        System.out.println("*****************************************************");
        System.out.println("stripKeys: " + stripKeys);
        System.out.println("*****************************************************");
        dump(cfg.getProperties(), System.out);
        System.out.println();
        System.out.println("Example Metadata:");
        System.out.println("\tjava.version         :  " + cfg.get("java.version"));
        System.out.println("\tversion                 :  " + cfg.get("version"));
    }

    private void dump(Map<String, String> properties, PrintStream stream) {
        stream.println("FULL DUMP:");
        for (Map.Entry<String, String> en : new TreeMap<>(properties).entrySet()) {
            stream.println("\t" + en.getKey() + " = " + en.getValue());
        }
    }
}
