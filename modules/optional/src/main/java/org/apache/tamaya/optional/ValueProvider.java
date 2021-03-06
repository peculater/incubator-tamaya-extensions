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
package org.apache.tamaya.optional;

/**
 * Simple (functional) interface, compatible with Java 7 that models a component that provides values.
 * It is the {@link EvaluationPolicy} that also must be passed, when creating an {@link OptionalConfiguration},
 * which is defining if values from this provider are overriding values from Tamaya (if available) or vice
 * versa. This provider interface must be implemented by the client that wants to optionally enhance its
 * code with optional Tamaya configuration support to createObject a bridge between his code and the values optionally
 * returned by Tamaya.
 */
@FunctionalInterface
public interface ValueProvider {

    /**
     * Access a typed createValue given a (non empty) key.
     * @param key the key, not null.
     * @param type the type, not null.
     * @param <T> the type
     * @return the createValue found, or null.
     */
    <T> T get(String key, Class<T> type);

}
