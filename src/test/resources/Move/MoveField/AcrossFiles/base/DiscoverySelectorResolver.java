/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.discoveryNEW;

import static java.lang.String.format;
import static org.junit.gen5.commons.util.ReflectionUtils.findMethods;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.gen5.commons.util.ReflectionUtils;
import org.junit.gen5.commons.util.StringUtils;
import org.junit.gen5.engine.EngineDiscoveryRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.discovery.ClassSelector;
import org.junit.gen5.engine.discovery.MethodSelector;
import org.junit.gen5.engine.discovery.UniqueIdSelector;
import org.junit.gen5.engine.junit5.descriptor.ClassTestDescriptor;
import org.junit.gen5.engine.junit5.discovery.JUnit5EngineDescriptor;

public class DiscoverySelectorResolver {

	private static final Logger LOG = Logger.getLogger(DiscoverySelectorResolver.class.getName());

	private final JUnit5EngineDescriptor engineDescriptor;
	private final Set<ElementResolver> resolvers = new HashSet<>();

	public DiscoverySelectorResolver(JUnit5EngineDescriptor engineDescriptor) {
		this.engineDescriptor = engineDescriptor;
		resolvers.add(new TestContainerResolver());
		resolvers.add(new TestMethodResolver());
	}

	public void resolveSelectors(EngineDiscoveryRequest request) {
		request.getSelectorsByType(ClassSelector.class).forEach(selector -> {
			resolveClass(selector.getTestClass());
		});
		request.getSelectorsByType(MethodSelector.class).forEach(selector -> {
			resolveMethod(selector.getTestClass(), selector.getTestMethod());
		});
		request.getSelectorsByType(UniqueIdSelector.class).forEach(selector -> {
			resolveUniqueId(UniqueId.parse(selector.getUniqueId()));
		});
		pruneTree();
	}

	private void resolveClass(Class<?> testClass) {
		Set<TestDescriptor> potentialParents = Collections.singleton(engineDescriptor);
		if (resolveElementWithChildren(testClass, potentialParents).isEmpty()) {
			LOG.warning(() -> {
				String classDescription = testClass.getName();
				return format("Class '%s' could not be resolved", classDescription);
			});
		}
	}

	private void resolveMethod(Class<?> testClass, Method testMethod) {
		Set<TestDescriptor> potentialParents = resolve(testClass, engineDescriptor);
		if (resolveElementWithChildren(testMethod, potentialParents).isEmpty()) {
			LOG.warning(() -> {
				String methodId = String.format("%s(%s)", testMethod.getName(),
					StringUtils.nullSafeToString(testMethod.getParameterTypes()));
				String methodDescription = testMethod.getDeclaringClass().getName() + "#" + methodId;
				return format("Method '%s' could not be resolved", methodDescription);
			});
		}
	}

	private void resolveUniqueId(UniqueId uniqueId) {
		List<UniqueId.Segment> segments = uniqueId.getSegments();
		segments.remove(0); // Ignore engine unique ID
		if (!resolveUniqueId(engineDescriptor, segments))
			LOG.warning(() -> {
				return format("Unique ID '%s' could not be resolved", uniqueId.getUniqueString());
			});
	}

	private void pruneTree() {
		TestDescriptor.Visitor removeDescriptorsWithoutTests = (descriptor, remove) -> {
			if (!descriptor.isRoot() && !descriptor.hasTests())
				remove.run();
		};
		engineDescriptor.accept(removeDescriptorsWithoutTests);
	}

	/**
	 * Return true if all segments of unique ID could be resolved
	 */
	private boolean resolveUniqueId(TestDescriptor parent, List<UniqueId.Segment> remainingSegments) {
		if (remainingSegments.isEmpty()) {
			resolveChildren(parent);
			return true;
		}

		UniqueId.Segment head = remainingSegments.remove(0);
		for (ElementResolver resolver : resolvers) {
			Optional<TestDescriptor> resolvedDescriptor = resolver.resolve(head, parent);
			if (!resolvedDescriptor.isPresent())
				continue;

			Optional<TestDescriptor> foundTestDescriptor = findTestDescriptorByUniqueId(
				resolvedDescriptor.get().getUniqueId());
			TestDescriptor descriptor = foundTestDescriptor.orElseGet(() -> {
				TestDescriptor newDescriptor = resolvedDescriptor.get();
				parent.addChild(newDescriptor);
				return newDescriptor;
			});
			return resolveUniqueId(descriptor, new ArrayList<>(remainingSegments));
		}
		return false;
	}

	private Set<TestDescriptor> resolveElementWithChildren(AnnotatedElement element,
			Set<TestDescriptor> potentialParents) {
		Set<TestDescriptor> resolvedDescriptors = new HashSet<>();
		potentialParents.forEach(parent -> {
			resolvedDescriptors.addAll(resolve(element, parent));
		});
		resolvedDescriptors.forEach(this::resolveChildren);
		return resolvedDescriptors;
	}

	private void resolveChildren(TestDescriptor descriptor) {
		if (descriptor instanceof ClassTestDescriptor) {
			Class<?> testClass = ((ClassTestDescriptor) descriptor).getTestClass();
			List<Method> testMethodCandidates = findMethods(testClass, method -> !ReflectionUtils.isPrivate(method),
				ReflectionUtils.MethodSortOrder.HierarchyDown);
			testMethodCandidates.forEach(
				method -> resolveElementWithChildren(method, Collections.singleton(descriptor)));
		}
	}

	private Set<TestDescriptor> resolve(AnnotatedElement element, TestDescriptor parent) {
		return resolvers.stream() //
				.map(resolver -> tryToResolveWithResolver(element, parent, resolver)) //
				.filter(testDescriptors -> !testDescriptors.isEmpty()) //
				.flatMap(Collection::stream) //
				.collect(Collectors.toSet());
	}

	private Set<TestDescriptor> tryToResolveWithResolver(AnnotatedElement element, TestDescriptor parent,
			ElementResolver resolver) {

		Set<TestDescriptor> resolvedDescriptors = resolver.resolve(element, parent);

		resolvedDescriptors.forEach(testDescriptor -> {
			Optional<TestDescriptor> existingTestDescriptor = findTestDescriptorByUniqueId(
				testDescriptor.getUniqueId());
			if (!existingTestDescriptor.isPresent()) {
				parent.addChild(testDescriptor);
			}
		});

		return resolvedDescriptors;
	}

	@SuppressWarnings("unchecked")
	private Optional<TestDescriptor> findTestDescriptorByUniqueId(UniqueId uniqueId) {
		return (Optional<TestDescriptor>) engineDescriptor.findByUniqueId(uniqueId);
	}

}
