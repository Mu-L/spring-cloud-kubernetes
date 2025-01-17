/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.commons.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import static org.springframework.cloud.kubernetes.commons.config.Constants.APPLICATION_PROPERTIES;
import static org.springframework.cloud.kubernetes.commons.config.Constants.APPLICATION_YAML;
import static org.springframework.cloud.kubernetes.commons.config.Constants.APPLICATION_YML;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.KEY_VALUE_TO_PROPERTIES;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.PROPERTIES_TO_MAP;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.throwingMerger;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.yamlParserGenerator;

/**
 * Processor that extracts data from an input, where input can be a single yaml/properties
 * file.
 *
 * @author Ioannis Canellos
 * @author Ali Shahbour
 * @author Michael Moudatsos
 */
public class SourceDataEntriesProcessor extends MapPropertySource {

	private static final Log LOG = LogFactory.getLog(SourceDataEntriesProcessor.class);

	public SourceDataEntriesProcessor(SourceData sourceData) {
		super(sourceData.sourceName(), sourceData.sourceData());
	}

	public static Map<String, Object> processAllEntries(Map<String, String> input, Environment environment) {

		Set<Map.Entry<String, String>> entrySet = input.entrySet();
		if (entrySet.size() == 1) {
			// we handle the case where the configmap contains a single "file"
			// in this case we don't care what the name of the file is
			Map.Entry<String, String> singleEntry = entrySet.iterator().next();
			String propertyName = singleEntry.getKey();
			String propertyValue = singleEntry.getValue();
			if (propertyName.endsWith(".yml") || propertyName.endsWith(".yaml")) {
				LOG.debug("The single property with name: [" + propertyName + "] will be treated as a yaml file");
				return yamlParserGenerator(environment).andThen(PROPERTIES_TO_MAP).apply(propertyValue);
			}
			else if (propertyName.endsWith(".properties")) {
				LOG.debug("The single property with name: [" + propertyName + "] will be treated as a properties file");
				return KEY_VALUE_TO_PROPERTIES.andThen(PROPERTIES_TO_MAP).apply(propertyValue);
			}
		}

		return defaultProcessAllEntries(input, environment);
	}

	private static Map<String, Object> defaultProcessAllEntries(Map<String, String> input, Environment environment) {

		return input.entrySet().stream().map(e -> extractProperties(e.getKey(), e.getValue(), environment))
				.flatMap(m -> m.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, throwingMerger(), HashMap::new));
	}

	private static Map<String, Object> extractProperties(String resourceName, String content, Environment environment) {

		if (resourceName.equals(APPLICATION_YAML) || resourceName.equals(APPLICATION_YML)) {
			return yamlParserGenerator(environment).andThen(PROPERTIES_TO_MAP).apply(content);
		}
		else if (resourceName.equals(APPLICATION_PROPERTIES)) {
			return KEY_VALUE_TO_PROPERTIES.andThen(PROPERTIES_TO_MAP).apply(content);
		}

		return Collections.singletonMap(resourceName, content);
	}

}
