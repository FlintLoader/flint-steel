/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2020-2021 HypherionSA and Contributors
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

import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.configuration.modules.ArtifactRef;

public abstract sealed class ModuleDependency permits SplitModuleDependency, SimpleModuleDependency {
	private final ArtifactRef artifact;
	protected final String group;
	protected final String name;
	protected final String version;
	@Nullable
	protected final String classifier;
	protected final String mappingsSuffix;
	protected final Project project;

	public ModuleDependency(ArtifactRef artifact, String mappingsSuffix, Project project) {
		this.artifact = artifact;
		this.group = artifact.group();
		this.name = artifact.name();
		this.version = artifact.version();
		this.classifier = artifact.classifier();
		this.mappingsSuffix = mappingsSuffix;
		this.project = project;
	}

	/**
	 * Returns true when the cache is invalid.
	 */
	public abstract boolean isCacheInvalid(Project project, @Nullable String variant);

	/**
	 * Write an artifact to the local cache.
	 */
	public abstract void copyToCache(Project project, Path path, @Nullable String variant) throws IOException;

	/**
	 * Apply the dependency to the project.
	 */
	public abstract void applyToProject(Project project);

	protected LocalMavenHelper createMaven(String name) {
		return new LocalMavenHelper(getRemappedGroup(), name, this.version, this.classifier, this.project);
	}

	public ArtifactRef getInputArtifact() {
		return artifact;
	}

	protected String getRemappedGroup() {
		return getMappingsPrefix() + "." + group;
	}

	private String getMappingsPrefix() {
		return mappingsSuffix.replace(".", "_").replace("-", "_").replace("+", "_");
	}

	public Path getInputFile() {
		return artifact.path();
	}

	public Path getWorkingFile(@Nullable String classifier) {
		final SteelGradleExtension extension = SteelGradleExtension.get(project);
		final String fileName = classifier == null ? String.format("%s-%s-%s.jar", getRemappedGroup(), name, version)
													: String.format("%s-%s-%s-%s.jar", getRemappedGroup(), name, version, classifier);

		return extension.getFiles().getProjectBuildCache().toPath().resolve("remapped_working").resolve(fileName);
	}

	@Override
	public String toString() {
		return "ModuleDependency{" + "group='" + group + '\'' + ", name='" + name + '\'' + ", version='" + version + '\'' + ", classifier='" + classifier + '\'' + '}';
	}
}
