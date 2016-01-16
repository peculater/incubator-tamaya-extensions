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
package org.apache.tamaya.mutableconfig.spi;

import org.apache.tamaya.ConfigException;
import org.apache.tamaya.mutableconfig.ConfigChangeRequest;

import java.net.URI;
import java.util.*;

/**
 * Base class for implementing a ConfigChangeRequest.
 */
public abstract class AbstractConfigChangeRequest implements ConfigChangeRequest {

    private final URI uri;
    private String requestID = UUID.randomUUID().toString();
    protected Map<String,String> properties = new HashMap<>();
    protected Set<String> removed = new HashSet<>();
    private boolean closed = false;

    protected AbstractConfigChangeRequest(URI uri){
        this.uri = Objects.requireNonNull(uri);
    }

    /**
     * Get the unique id of this change reqeust (UUID).
     * @return the unique id of this change reqeust (UUID).
     */
    @Override
    public String getRequestID(){
        return requestID;
    }

    @Override
    public final URI getBackendURI() {
        return uri;
    }

    @Override
    public boolean isWritable(String keyExpression) {
        return true;
    }

    @Override
    public boolean isRemovable(String keyExpression) {
        return true;
    }

    protected Map<String,String> getProperties(){
        return properties;
    }

    protected Set<String> getRemoved(){
        return removed;
    }

    @Override
    public abstract boolean exists(String keyExpression);

    @Override
    public ConfigChangeRequest put(String key, String value) {
        checkClosed();
        this.properties.put(key, value);
        return this;
    }

    @Override
    public ConfigChangeRequest putAll(Map<String, String> properties) {
        checkClosed();
        this.properties.putAll(properties);
        return this;
    }

    @Override
    public ConfigChangeRequest remove(String... keys) {
        checkClosed();
        for(String k:keys) {
            this.removed.add(k);
        }
        return this;
    }

    @Override
    public ConfigChangeRequest remove(Collection<String> keys) {
        checkClosed();
        for(String k:keys) {
            this.removed.add(k);
        }
        return this;
    }

    @Override
    public ConfigChangeRequest removeAll(String... keys) {
        checkClosed();
        for(String k:keys) {
            this.removed.add(k);
        }
        return this;
    }

    @Override
    public final void commit() {
        checkClosed();
        commitInternal();
        closed = true;
    }

    protected abstract void commitInternal();

    @Override
    public final void cancel() {
        checkClosed();
        closed = true;
    }

    @Override
    public final boolean isClosed() {
        return closed;
    }

    protected void checkClosed(){
        if(closed){
            throw new ConfigException("Change request already closed.");
        }
    }
}
