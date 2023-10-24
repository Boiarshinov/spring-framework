/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SimpleInstantiationStrategy}.
 *
 * @author Stephane Nicoll
 */
class SimpleInstantiationStrategyTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final SimpleInstantiationStrategy strategy = new SimpleInstantiationStrategy();

	@Test
	void instantiateWithNoArg() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		Object simpleBean = instantiate(bd, new SampleFactory(),
				method(SampleFactory.class, "simpleBean"));
		assertThat(simpleBean).isEqualTo("Hello");
	}

	@Test
	void instantiateWitArgs() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		Object simpleBean = instantiate(bd, new SampleFactory(),
				method(SampleFactory.class, "beanWithTwoArgs"), "Test", 42);
		assertThat(simpleBean).isEqualTo("Test42");
	}

	@Test
	void instantiateWithNullValueReturnsNullBean() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		Object simpleBean = instantiate(bd, new SampleFactory(),
				method(SampleFactory.class, "cloneBean"), new Object[] { null });
		assertThat(simpleBean).isNotNull().isInstanceOf(NullBean.class);
	}

	@Test
	void instantiateWithArgumentTypeMismatch() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() -> instantiate(
						bd, new SampleFactory(),
						method(SampleFactory.class, "beanWithTwoArgs"), 42, "Test"))
				.withMessageContaining("Illegal arguments to factory method 'beanWithTwoArgs'")
				.withMessageContaining("args: 42,Test");
	}

	@Test
	void instantiateWithException() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() -> instantiate(
						bd, new SampleFactory(),
						method(SampleFactory.class, "errorBean"), "This a test message"))
				.withMessageContaining("Factory method 'errorBean' threw exception")
				.withMessageContaining("This a test message")
				.havingCause().isInstanceOf(IllegalStateException.class).withMessage("This a test message");
	}

	private Object instantiate(RootBeanDefinition bd, Object factory, Method method, Object... args) {
		return this.strategy.instantiate(bd, "simpleBean", this.beanFactory,
				factory, method, args);
	}

	private static Method method(Class<?> target, String methodName) {
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(
				target, method -> methodName.equals(method.getName()));
		assertThat(methods).as("No unique method named " + methodName + " found of " + target.getName())
				.hasSize(1);
		return methods[0];
	}


	static class SampleFactory {

		String simpleBean() {
			return "Hello";
		}

		String beanWithTwoArgs(String first, Integer second) {
			return first + second;
		}

		String cloneBean(String arg) {
			return arg;
		}

		String errorBean(String msg) {
			throw new IllegalStateException(msg);
		}

	}
}