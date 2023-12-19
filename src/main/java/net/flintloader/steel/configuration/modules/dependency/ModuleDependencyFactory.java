/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2016-2021 Flint Loader Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.flintloader.steel.configuration.modules.dependency;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.jetbrains.annotations.Nullable;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.configuration.modules.ArtifactRef;
import net.flintloader.steel.configuration.modules.JarSplitter;
import net.flintloader.steel.util.AttributeHelper;

public class ModuleDependencyFactory {
	private static final String TARGET_ATTRIBUTE_KEY = "steel-target";

	public static ModuleDependency create(ArtifactRef artifact, Configuration targetConfig, @Nullable Configuration targetClientConfig, String mappingsSuffix, Project project) {
		if (targetClientConfig != null && SteelGradleExtension.get(project).getSplitModuleDependencies().get()) {
			final Optional<JarSplitter.Target> cachedTarget = readTarget(artifact);
			JarSplitter.Target target;

			if (cachedTarget.isPresent()) {
				target = cachedTarget.get();
			} else {
				target = new JarSplitter(artifact.path()).analyseTarget();
				writeTarget(artifact, target);
			}

			if (target != null) {
				return new SplitModuleDependency(artifact, mappingsSuffix, targetConfig, targetClientConfig, target, project);
			}
		}

		return new SimpleModuleDependency(artifact, mappingsSuffix, targetConfig, project);
	}

	private static Optional<JarSplitter.Target> readTarget(ArtifactRef artifact) {
		try {
			return AttributeHelper.readAttribute(artifact.path(), TARGET_ATTRIBUTE_KEY).map(s -> {
				if ("null".equals(s)) {
					return null;
				}

				return JarSplitter.Target.valueOf(s);
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read artifact target attribute", e);
		}
	}

	private static void writeTarget(ArtifactRef artifact, JarSplitter.Target target) {
		final String value = target != null ? target.name() : "null";

		try {
			AttributeHelper.writeAttribute(artifact.path(), TARGET_ATTRIBUTE_KEY, value);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write artifact target attribute", e);
		}
	}
}
