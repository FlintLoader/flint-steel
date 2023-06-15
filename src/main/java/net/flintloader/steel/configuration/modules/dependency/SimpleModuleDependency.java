/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2022 HypherionSA and Contributors
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
import java.nio.file.Path;
import java.util.Objects;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.jetbrains.annotations.Nullable;

import net.flintloader.steel.configuration.modules.ArtifactRef;

// Single jar in and out
public final class SimpleModuleDependency extends ModuleDependency {
	private final Configuration targetConfig;
	private final LocalMavenHelper maven;

	public SimpleModuleDependency(ArtifactRef artifact, String mappingsSuffix, Configuration targetConfig, Project project) {
		super(artifact, mappingsSuffix, project);
		this.targetConfig = Objects.requireNonNull(targetConfig);
		this.maven = createMaven(name);
	}

	@Override
	public boolean isCacheInvalid(Project project, @Nullable String variant) {
		return !maven.exists(variant);
	}

	@Override
	public void copyToCache(Project project, Path path, @Nullable String variant) throws IOException {
		maven.copyToMaven(path, variant);
	}

	@Override
	public void applyToProject(Project project) {
		project.getDependencies().add(targetConfig.getName(), maven.getNotation());
	}
}