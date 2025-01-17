/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;

import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.cache.QuartzEndpointWebExtension;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzEndpointAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class QuartzEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(QuartzEndpointAutoConfiguration.class));

	@Test
	void endpointIsAutoConfigured() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
				.withPropertyValues("management.endpoints.web.exposure.include=quartz")
				.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class));
	}

	@Test
	void endpointIsNotAutoConfiguredIfSchedulerIsNotAvailable() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=quartz")
				.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointNotAutoConfiguredWhenNotExposed() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
				.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointCanBeDisabled() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
				.withPropertyValues("management.endpoint.quartz.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointBacksOffWhenUserProvidedEndpointIsPresent() {
		this.contextRunner.withUserConfiguration(CustomEndpointConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class).hasBean("customEndpoint"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomEndpointConfiguration {

		@Bean
		CustomEndpoint customEndpoint() {
			return new CustomEndpoint();
		}

	}

	private static final class CustomEndpoint extends QuartzEndpoint {

		private CustomEndpoint() {
			super(mock(Scheduler.class));
		}

	}

	@Test
	void runWhenOnlyExposedOverJmxShouldHaveEndpointBeanWithoutWebExtension() {
    this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
            .withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.DEBUG))
            .withPropertyValues("spring.jmx.enabled=true", "management.endpoints.jmx.exposure.include=quartz")
            .run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class)
                    .doesNotHaveBean(QuartzEndpointWebExtension.class));
}

}
