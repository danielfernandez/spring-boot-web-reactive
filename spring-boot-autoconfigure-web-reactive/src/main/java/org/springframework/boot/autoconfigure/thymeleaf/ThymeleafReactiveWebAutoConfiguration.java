/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.thymeleaf;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.reactiveweb.ReactiveHttpServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.DispatcherHandler;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring5.ISpringWebReactiveTemplateEngine;
import org.thymeleaf.spring5.SpringWebReactiveTemplateEngine;
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Thymeleaf's Reactive
 * Web mechanisms.
 *
 * @author Daniel Fernández
 */
@Configuration
@EnableConfigurationProperties(ThymeleafProperties.class)
@ConditionalOnClass(TemplateMode.class)
// NOT if merged with ThymeleafAutoConfiguration, this @AutoConfigureAfter should be reshaped as
// @AutoConfigureAfter({WebMvcAutoConfiguration.class, ReactiveHttpServerAutoConfiguration.class})
@AutoConfigureAfter({ReactiveHttpServerAutoConfiguration.class, ThymeleafAutoConfiguration.class})
public class ThymeleafReactiveWebAutoConfiguration {

	/*
	 * No need to configure the defaultTemplateResolver, as it should have already been configured
	 * by the ThymeleafAutoConfiguration class.
	 *
	 * NOTE if merged with ThymeleafAutoConfiguration, this comment should be removed
	 */


	/*
	 * NOTE if merged with ThymeleafAutoConfiguration, this inner class should simply be added inside
	 * the class in order to initialize all the reactive-web parts for Thymeleaf.
	 */
	@Configuration
	@ConditionalOnClass({DispatcherHandler.class, HttpHandler.class})
	@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
	static class ThymeleafReactiveViewResolverConfiguration {

		private final ThymeleafProperties properties;

		private final Collection<ITemplateResolver> templateResolvers;

		private final Collection<IDialect> dialects;

		ThymeleafReactiveViewResolverConfiguration(
				ThymeleafProperties properties,
				Collection<ITemplateResolver> templateResolvers,
				ObjectProvider<Collection<IDialect>> dialectsProvider) {
			this.properties = properties;
			this.templateResolvers = templateResolvers;
			this.dialects = dialectsProvider.getIfAvailable();
		}

		@Bean
		@ConditionalOnMissingBean(name = "thymeleafReactiveViewResolver")
		public ThymeleafReactiveViewResolver thymeleafViewResolver(ISpringWebReactiveTemplateEngine templateEngine) {

			// TODO * SEVERAL PROPERTIES COMMENTED OUT HERE because the ThymeleafProperties class is modelled
			// TODO   after the needs of Thymeleaf's Spring Web MVC integration (and not Spring Web Reactive). The
			// TODO   ThymeleafProperties and the configuration "spring.thymeleaf" prefix will probably need some
			// TODO   kind of refactoring in order to give proper support to the configuration of this
			// TODO   ThymeleafReactiveViewResolver instance.
			
			ThymeleafReactiveViewResolver resolver = new ThymeleafReactiveViewResolver();
			resolver.setTemplateEngine(templateEngine);
//			resolver.setCharacterEncoding(this.properties.getEncoding().name());
//			resolver.setContentType(appendCharset(this.properties.getContentType(),
//					resolver.getCharacterEncoding()));
			resolver.setExcludedViewNames(this.properties.getExcludedViewNames());
			resolver.setViewNames(this.properties.getViewNames());
			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
//			resolver.setCache(this.properties.isCache());
			return resolver;
		}

		private String appendCharset(MimeType type, String charset) {
			if (type.getCharset() != null) {
				return type.toString();
			}
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			parameters.put("charset", charset);
			parameters.putAll(type.getParameters());
			return new MimeType(type, parameters).toString();
		}

		/*
		 * This bean is kept inside the ThymeleafReactiveViewResolverConfiguration because an
		 * ISpringWebReactiveTemplateEngine makes no real sense in a non-reactive-web scenario.
		 */
		@Bean
		@ConditionalOnMissingBean(ISpringWebReactiveTemplateEngine.class)
		public ISpringWebReactiveTemplateEngine templateEngine() {
			SpringWebReactiveTemplateEngine engine = new SpringWebReactiveTemplateEngine();
			for (ITemplateResolver templateResolver : this.templateResolvers) {
				engine.addTemplateResolver(templateResolver);
			}
			if (!CollectionUtils.isEmpty(this.dialects)) {
				for (IDialect dialect : this.dialects) {
					engine.addDialect(dialect);
				}
			}
			return engine;
		}

	}

}
