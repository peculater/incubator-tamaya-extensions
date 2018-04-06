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
package org.apache.tamaya.events.spi;

import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.data.Offset;
import org.junit.Test;

/**
 *
 * @author William.Lieurance 2018-03-26
 */
public class BaseConfigEventTest {
    

    @Test
    public void testBaseConfigEvent() {
        SomeBaseConfigEvent someEvent = new SomeBaseConfigEvent();
        //Within an hour anyhow
        assertThat(someEvent.getTimestamp()).isGreaterThan(0L);
        assertThat(someEvent.getResourceType()).isEqualTo(String.class);
        assertThat(UUID.fromString(someEvent.getVersion())).isNotNull();
    }

    public class SomeBaseConfigEvent extends BaseConfigEvent {

        public SomeBaseConfigEvent() {
            super("SomeString", String.class);
        }
    }
    
}
