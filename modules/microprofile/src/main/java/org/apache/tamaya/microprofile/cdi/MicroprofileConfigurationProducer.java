/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tamaya.microprofile.cdi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Provider;

import org.apache.tamaya.ConfigException;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.apache.tamaya.TypeLiteral;
import org.apache.tamaya.spi.ConversionContext;
import org.apache.tamaya.spi.PropertyConverter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * Producer bean for configuration properties.
 */
@ApplicationScoped
public class MicroprofileConfigurationProducer {

    private static final Logger LOGGER = Logger.getLogger(MicroprofileConfigurationProducer.class.getName());

    @Produces
    @ConfigProperty
    public Object resolveAndConvert(final InjectionPoint injectionPoint) {
        LOGGER.finest( () -> "Inject: " + injectionPoint);
        final ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        String key = annotation.name();
        if(key.isEmpty()){
            key = getDefaultKey(injectionPoint);
        }

        // unless the extension is not installed, this should never happen because the extension
        // enforces the resolvability of the configuration

        String defaultTextValue = annotation.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE) ? null : annotation.defaultValue();
        ConversionContext conversionContext = createConversionContext(key, injectionPoint);
        Object value = resolveValue(defaultTextValue, conversionContext, injectionPoint);
        if (value == null) {
            throw new ConfigException(String.format(
                    "Can't resolve any of the possible config keys: %s to the required target type: %s, supported formats: %s",
                    key, conversionContext.getTargetType(), conversionContext.getSupportedFormats().toString()));
        }
        LOGGER.finest(String.format("Injecting %s for key %s in class %s", key, value.toString(), injectionPoint.toString()));
        return value;
    }

    @SuppressWarnings("rawtypes")
	static String getDefaultKey(InjectionPoint injectionPoint) {
        Class declaringType = injectionPoint.getMember().getDeclaringClass();
        return declaringType.getCanonicalName() + "." + injectionPoint.getMember().getName();
    }

    static ConversionContext createConversionContext(String key, InjectionPoint injectionPoint) {
        final Type targetType = injectionPoint.getAnnotated().getBaseType();
        Configuration config = ConfigurationProvider.getConfiguration();
        ConversionContext.Builder builder = new ConversionContext.Builder(config,
                ConfigurationProvider.getConfiguration().getContext(), key, TypeLiteral.of(targetType));
        if(targetType instanceof ParameterizedType){
            ParameterizedType pt = (ParameterizedType)targetType;
            if(pt.getRawType().equals(Provider.class)) {
                builder = new ConversionContext.Builder(config,
                        ConfigurationProvider.getConfiguration().getContext(), key,
                        TypeLiteral.of(pt.getActualTypeArguments()[0]));
            }
        }
        if (injectionPoint.getMember() instanceof AnnotatedElement) {
            AnnotatedElement annotated = (AnnotatedElement)injectionPoint.getMember();
            if(annotated.isAnnotationPresent(ConfigProperty.class)) {
                builder.setAnnotatedElement(annotated);
            }
        }else if(injectionPoint.getMember() instanceof Method){
            Method method = (Method)injectionPoint.getMember();
            for(Type type:method.getParameterTypes()){
                if(type instanceof AnnotatedElement){
                    AnnotatedElement annotated = (AnnotatedElement)type;
                    if(annotated.isAnnotationPresent(ConfigProperty.class)) {
                        builder.setAnnotatedElement(annotated);
                    }
                }
            }
        }
        return builder.build();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Object resolveValue(String defaultTextValue, ConversionContext context, InjectionPoint injectionPoint) {
        Config config = ConfigProviderResolver.instance().getConfig();
        String textValue = config.getOptionalValue(context.getKey(), String.class).orElse(defaultTextValue);
        if(String.class.equals(context.getTargetType().getRawType())){
            return textValue;
        }
        Object value = null;
        if (textValue != null || Optional.class.equals(context.getTargetType().getRawType())) {
            LOGGER.log(Level.FINEST, () -> "Converting KEY: " + context.getKey() + "("+context.getTargetType()+"), textValue: " + textValue);
			List<PropertyConverter> converters = ConfigurationProvider.getConfiguration().getContext()
                    .getPropertyConverters((TypeLiteral)context.getTargetType());
            for (PropertyConverter<Object> converter : converters) {
                try {
                    value = converter.convert(textValue, context);
                    if (value != null) {
                        LOGGER.log(Level.FINEST, "Parsed default value from '" + textValue + "' into " +
                                injectionPoint);
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINEST, "Failed to convert value '" + textValue + "' for " +
                            injectionPoint, e);
                }
            }
        }
        return value;
    }

    @Produces
    public Config getConfiguration(){
        return ConfigProvider.getConfig();
    }

    @Produces
    public ConfigBuilder getConfigBuilder(){
        return ConfigProviderResolver.instance().getBuilder();
    }

}
