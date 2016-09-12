/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.logging.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.jboss.logging.Logger;

/**
 * An implementation of Apache Commons Logging {@code LogFactory} for JBoss Logging.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JBossLogFactory extends LogFactory {
    private static final Collection<String> UNSUPPORTED_PROPERTIES = Arrays.asList(
            LogFactory.FACTORY_PROPERTY,
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.log"
    );

    // Note that this is effectively static. This could be problematic with if a log manager uses different contexts.
    // However it's not worth the overhead of trying to determine when a new attribute map would be required given that
    // it's like not a common API.
    private final Map<String, Object> attributeMap = Collections.synchronizedMap(new HashMap<String, Object>());
    private final Logger logger = Logger.getLogger(JBossLogFactory.class.getPackage().getName());

    @Override
    public Object getAttribute(final String name) {
        return getAttributeMap().get(name);
    }


    @Override
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public String[] getAttributeNames() {
        final Map<String, Object> attributes = getAttributeMap();
        final String[] names;
        synchronized (attributes) {
            final Set<String> s = attributes.keySet();
            names = s.toArray(new String[s.size()]);
        }
        return names;
    }

    @Override
    public Log getInstance(final Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    @Override
    public Log getInstance(final String name) throws LogConfigurationException {
        // We always return a new log instance because this factory is effectively static. See the LogFactory.Holder.
        // For details on why this done see WFCORE-254. If performance becomes an issue we can readdress the ability to
        // store loggers in some kind of container.
        return new JBossLog(name);
    }

    @Override
    public void release() {
        // Clear the attributes
        getAttributeMap().clear();
    }

    @Override
    public void removeAttribute(final String name) {
        getAttributeMap().remove(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        final Map<String, Object> attributes = getAttributeMap();
        if (value == null) {
            attributes.remove(name);
        } else {
            if (!(value instanceof String)) {
                logger.warnf("Attribute values must be of type java.lang.String. Attribute %s with value %s will be ignored.", name, value);
            } else if (UNSUPPORTED_PROPERTIES.contains(name)) {
                logger.warnf("Attribute %s is not supported. Value %s will be ignored.", name, value);
            } else {
                attributes.put(name, value);
            }
        }
    }

    private Map<String, Object> getAttributeMap() {
        return attributeMap;
    }
}
