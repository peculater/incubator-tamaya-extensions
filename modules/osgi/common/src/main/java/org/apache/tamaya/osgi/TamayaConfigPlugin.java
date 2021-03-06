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
package org.apache.tamaya.osgi;

import org.apache.tamaya.osgi.commands.TamayaConfigService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tamaya plugin that updates/extends the component configurations managed
 * by {@link ConfigurationAdmin}, based on the configured {@link Policy}.
 */
public class TamayaConfigPlugin implements TamayaConfigService, BundleListener, ServiceListener {
    static final String COMPONENTID = "TamayaConfigPlugin";
    /**
     * the logger.
     */
    private static final Logger LOG = Logger.getLogger(TamayaConfigPlugin.class.getName());

    public static final String TAMAYA_POLICY_PROP = "tamaya-policy";
    public static final String TAMAYA_POLICY_MANIFEST = "Tamaya-Policy";
    public static final String TAMAYA_CUSTOM_ROOT_PROP = "tamaya-config-root";
    public static final String TAMAYA_CUSTOM_ROOT_MANIFEST = "Tamaya-Config-Root";
    public static final String TAMAYA_ENABLED_PROP = "tamaya-enabled";
    public static final String TAMAYA_ENABLED_MANIFEST = "Tamaya-Enabled";
    public static final String TAMAYA_AUTO_UPDATE_ENABLED_PROP = "tamaya-update-enabled";
    public static final String TAMAYA_AUTO_UPDATE_ENABLED_MANIFEST = "Tamaya-Update-Enabled";
    private boolean enabledByDefault = false;

    private Policy defaultPolicy = Policy.OVERRIDE;

    private ConfigChanger configChanger;
    private boolean autoUpdateEnabled;

    @Override
    public void serviceChanged(ServiceEvent event) {
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
            case ServiceEvent.MODIFIED:
                configureService(event);
                break;
            case ServiceEvent.UNREGISTERING:
                // unconfigure...? Currently nothing here.
                break;
            default:
                break;
        }
    }


    /**
     * Create a new getConfig.
     *
     * @param context the OSGI context
     */
    public TamayaConfigPlugin(BundleContext context) {
        configChanger = new ConfigChanger(context);
        Dictionary<String, Object> props = getPluginConfig();
        Backups.restore(props);
        ConfigHistory.restore(props);
        initDefaultEnabled(props);
        initAutoUpdateEnabled(props);
        initDefaultOpMode(props);
        initConfigs();
    }

    @Override
    public void setAutoUpdateEnabled(boolean enabled) {
        this.autoUpdateEnabled = enabled;
        setConfigValue(TAMAYA_AUTO_UPDATE_ENABLED_PROP, enabled);
    }

    @Override
    public void setTamayaEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
        setConfigValue(TAMAYA_ENABLED_PROP, enabledByDefault);
    }

    @Override
    public boolean isTamayaEnabledByDefault() {
        return enabledByDefault;
    }

    @Override
    public Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    @Override
    public void setDefaultPolicy(Policy mode) {
        this.defaultPolicy = Objects.requireNonNull(mode);
        setConfigValue(Policy.class.getSimpleName(), defaultPolicy.toString());
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTING:
            case BundleEvent.LAZY_ACTIVATION:
                configureBundle(event.getBundle());
                break;
            default:
                break;
        }
    }

    private void initConfigs() {
        // Check for matching bundles already installed...
        for (Bundle bundle : configChanger.getContext().getBundles()) {
            switch (bundle.getState()) {
                case Bundle.ACTIVE:
                    configureBundle(bundle);
                    break;
                default:
                    break;
            }
        }
    }

    private void configureService(ServiceEvent event) {
        // Optional MANIFEST entries
        Bundle bundle = event.getServiceReference().getBundle();
        if (!isBundleEnabled(bundle)) {
            return;
        }
        String pid = (String) event.getServiceReference().getProperty(Constants.SERVICE_PID);
        if (pid == null) {
            LOG.finest("No service pid for: " + event.getServiceReference());
            return;
        }
        configChanger.configure(pid, event.getServiceReference().getBundle(), defaultPolicy, false, false);
        Dictionary<String, Object> props = getPluginConfig();
        Backups.save(props);
        ConfigHistory.save(props);
        setPluginConfig(props);
    }

    @Override
    public Dictionary<String, Object> updateConfig(String pid) {
        return updateConfig(pid, defaultPolicy, false, false);
    }

    @Override
    public Dictionary<String, Object> updateConfig(String pid, boolean dryRun) {
        return updateConfig(pid, defaultPolicy, false, dryRun);
    }

    @Override
    public Dictionary<String, Object> updateConfig(String pid, Policy opMode, boolean explicitMode, boolean dryRun) {
        if (dryRun) {
            return configChanger.configure(pid, null, opMode, explicitMode, true);
        } else {
            LOG.fine("Updating getConfig for pid...: " + pid);
            Dictionary<String, Object> result = configChanger.configure(pid, null, opMode, explicitMode, false);
            Dictionary<String, Object> props = getPluginConfig();
            Backups.save(props);
            ConfigHistory.save(props);
            setPluginConfig(props);
            return result;
        }
    }

    private void configureBundle(Bundle bundle) {
        if (!isBundleEnabled(bundle)) {
            return;
        }
        String tamayaPid = bundle.getHeaders().get(TAMAYA_CUSTOM_ROOT_MANIFEST);
        String pid = tamayaPid != null ? tamayaPid : bundle.getSymbolicName();
        if (pid == null) {
            pid = bundle.getLocation();
        }
        if (pid == null) {
            LOG.finest(() -> "No PID/location for bundle " + bundle.getSymbolicName() + '(' + bundle.getBundleId() + ')');
            return;
        }
        configChanger.configure(pid, bundle, defaultPolicy, false, false);
        Dictionary<String, Object> props = getPluginConfig();
        Backups.save(props);
        ConfigHistory.save(props);
        setPluginConfig(props);
    }

    @Override
    public boolean isBundleEnabled(Bundle bundle) {
        // Optional MANIFEST entries
        String bundleEnabledVal = bundle.getHeaders().get(TAMAYA_ENABLED_MANIFEST);
        if (bundleEnabledVal == null && !enabledByDefault) {
            LOG.finest("tamaya.enabled=false: not configuring bundle: " + bundle.getSymbolicName());
            return false;
        }
        if (bundleEnabledVal != null && !Boolean.parseBoolean(bundleEnabledVal)) {
            LOG.finest("Bundle is explcitly disabled for Tamaya: " + bundle.getSymbolicName());
            return false;
        }
        if (bundleEnabledVal != null && Boolean.parseBoolean(bundleEnabledVal)) {
            LOG.finest("Bundle is explicitly enabled for Tamaya: " + bundle.getSymbolicName());
            return true;
        }
        return true;
    }

    // REVIEW is this method still needed at all?
    private boolean isAutoUpdateEnabled(Bundle bundle, Dictionary<String, Object> props) {
        Object enabledVal = props.get(TAMAYA_AUTO_UPDATE_ENABLED_PROP);
        if (enabledVal != null) {
            return Boolean.parseBoolean(enabledVal.toString());
        }
        if (bundle != null) {
            enabledVal = bundle.getHeaders().get(TAMAYA_AUTO_UPDATE_ENABLED_MANIFEST);
            if (enabledVal != null) {
                return Boolean.parseBoolean(enabledVal.toString());
            }
        }
        return this.autoUpdateEnabled;
    }

    private void initAutoUpdateEnabled(Dictionary<String, Object> props) {
        Object enabledVal = props.get(TAMAYA_AUTO_UPDATE_ENABLED_PROP);
        if (enabledVal == null && System.getProperty(TAMAYA_AUTO_UPDATE_ENABLED_PROP) != null) {
            enabledVal = Boolean.parseBoolean(System.getProperty(TAMAYA_AUTO_UPDATE_ENABLED_PROP));
        }
        if (enabledVal != null) {
            this.autoUpdateEnabled = Boolean.parseBoolean(enabledVal.toString());
        }
        if (this.autoUpdateEnabled) {
            LOG.info("Tamaya Automatic Config Update is enabled by default.");
        } else {
            LOG.info("Tamaya Automatic Config Update is disabled by default.");
        }
    }

    private void initDefaultEnabled(Dictionary<String, Object> props) {
        Object enabledVal = props.get(TAMAYA_ENABLED_PROP);
        if (enabledVal == null && System.getProperty(TAMAYA_ENABLED_PROP) != null) {
            enabledVal = Boolean.parseBoolean(System.getProperty(TAMAYA_ENABLED_PROP));
        }
        if (enabledVal != null) {
            this.enabledByDefault = Boolean.parseBoolean(enabledVal.toString());
        }
        if (this.enabledByDefault) {
            LOG.info("Tamaya Config is enabled by default. Add Tamaya-Enabled to your bundle manifests to enable it.");
        } else {
            LOG.info("Tamaya Config is not enabled by default. Add Tamaya-Disabled to your bundle manifests to disable it.");
        }
    }

    private void initDefaultOpMode(Dictionary<String, Object> props) {
        String opVal = (String) props.get(Policy.class.getSimpleName());
        if (opVal != null) {
            try {
                defaultPolicy = Policy.valueOf(opVal);
            } catch (Exception e) {
                LOG.warning("Invalid Policy: " + opVal + ", using default: " + defaultPolicy);
            }
        }
    }

    Dictionary<String, Object> getPluginConfig() {
        Configuration config;
        try {
            config = configChanger.getConfigurationAdmin().getConfiguration(COMPONENTID);
            Dictionary<String, Object> props;
            if (config != null
                    && config.getProperties() != null) {
                props = config.getProperties();
            } else {
                props = new Hashtable<>();
            }
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("No Tamaya plugin config.", e);
        }
    }

    void setPluginConfig(Dictionary<String, Object> props) {
        Configuration config;
        try {
            config = configChanger.getConfigurationAdmin().getConfiguration(COMPONENTID);
            if (config != null) {
                config.update(props);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to write Tamaya plugin config.", e);
        }
    }

    void setConfigValue(String key, Object value) {
        try {
            Dictionary<String, Object> props = getPluginConfig();
            if (props != null) {
                props.put(key, value);
                setPluginConfig(props);
                LOG.finest("Updated Tamaya Plugin createValue: " + key + "=" + value);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error writing Tamaya config createValue: " + key, e);
        }
    }

    Object getConfigValue(String key) {
        try {
            Dictionary<String, Object> props = getPluginConfig();
            if (props != null) {
                return props.get(key);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error reading Tamaya config createValue.", e);
        }
        return null;
    }


    public org.apache.tamaya.Configuration getTamayaConfiguration(String root) {
        return configChanger.getTamayaConfiguration(root);
    }

    @Override
    public boolean isAutoUpdateEnabled() {
        return this.autoUpdateEnabled;
    }

    @Override
    public Dictionary<String, ?> getBackup(String pid) {
        return Backups.get(pid);
    }

    @Override
    public Set<String> getBackupPids() {
        return Backups.getPids();
    }

    @Override
    public boolean restoreBackup(String pid) {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> config = (Dictionary<String, Object>) Backups.get(pid);
        if (config == null) {
            return false;
        }
        try {
            this.configChanger.restoreBackup(pid, config);
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error restoring backup for PID: " + pid, e);
            return false;
        }
    }

    @Override
    public boolean createBackup(String pid) {
        if (!Backups.contains(pid)) {
            Backups.set(pid, getOSGIConfiguration(pid, null));
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteBackup(String pid) {
        if (Backups.contains(pid)) {
            Backups.remove(pid);
            return true;
        }
        return false;
    }

    @Override
    public void setMaxHistorySize(int maxHistory) {
        ConfigHistory.setMaxHistory(maxHistory);
    }

    @Override
    public int getMaxHistorySize() {
        return ConfigHistory.getMaxHistory();
    }

    @Override
    public List<ConfigHistory> getHistory() {
        return ConfigHistory.getHistory();
    }

    @Override
    public void clearHistory() {
        ConfigHistory.clearHistory();
    }

    @Override
    public void clearHistory(String pid) {
        ConfigHistory.clearHistory(pid);
    }

    @Override
    public List<ConfigHistory> getHistory(String pid) {
        return ConfigHistory.getHistory(pid);
    }

    @Override
    public Dictionary<String, Object> getOSGIConfiguration(String pid, String section) {
        try {
            Configuration config = configChanger.getConfigurationAdmin().getConfiguration(pid);
            Dictionary<String, Object> props;
            if (config == null
                    || config.getProperties() == null) {
                return null;
            }
            props = config.getProperties();
            if (section != null) {
                return filter(props, section);
            }
            return props;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error reading OSGI config for PID: " + pid, e);
            return null;
        }
    }

    @Override
    public boolean containsBackup(String pid) {
        return Backups.contains(pid);
    }

    private Dictionary<String, Object> filter(Dictionary<String, Object> props, String section) {
        Hashtable<String, Object> result = new Hashtable<>();
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith(section)) {
                result.put(key, props.get(key));
            }
        }
        return result;
    }

}
