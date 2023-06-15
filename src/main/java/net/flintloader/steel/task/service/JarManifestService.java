/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2021-2022 HypherionSA and Contributors
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

package net.flintloader.steel.task.service;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.util.GradleVersion;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.SteelGradlePlugin;
import net.flintloader.steel.util.Constants;

import net.fabricmc.tinyremapper.TinyRemapper;

public abstract class JarManifestService implements BuildService<JarManifestService.Params> {
	interface Params extends BuildServiceParameters {
		Property<String> getGradleVersion();
		Property<String> getSteelVersion();
		Property<String> getMCEVersion();
		Property<String> getMinecraftVersion();
		Property<String> getTinyRemapperVersion();
		Property<String> getFlintLoaderVersion();
		Property<MixinVersion> getMixinVersion();
	}

	public static synchronized Provider<JarManifestService> get(Project project) {
		return project.getGradle().getSharedServices().registerIfAbsent("SteelJarManifestService:" + project.getName(), JarManifestService.class, spec -> {
			spec.parameters(params -> {
				SteelGradleExtension extension = SteelGradleExtension.get(project);
				Optional<String> tinyRemapperVersion = Optional.ofNullable(TinyRemapper.class.getPackage().getImplementationVersion());

				params.getGradleVersion().set(GradleVersion.current().getVersion());
				params.getSteelVersion().set(SteelGradlePlugin.STEEL_VERSION);
				params.getMCEVersion().set(Constants.Dependencies.Versions.MIXIN_COMPILE_EXTENSIONS);
				params.getMinecraftVersion().set(extension.getMinecraftProvider().minecraftVersion());
				params.getTinyRemapperVersion().set(tinyRemapperVersion.orElse("unknown"));
				params.getFlintLoaderVersion().set(getLoaderVersion(project).orElse("unknown"));
				params.getMixinVersion().set(getMixinVersion(project).orElse(new MixinVersion("unknown", "unknown")));
			});
		});
	}

	public void apply(Manifest manifest, Map<String, String> extraValues) {
		// Don't set when running the reproducible build tests as it will break them when anything updates
		if (Boolean.getBoolean("steel.test.reproducible")) {
			return;
		}

		Attributes attributes = manifest.getMainAttributes();
		Params p = getParameters();

		attributes.putValue("Flint-Gradle-Version", p.getGradleVersion().get());
		attributes.putValue("Flint-Steel-Version", p.getSteelVersion().get());
		attributes.putValue("Flint-Mixin-Compile-Extensions-Version", p.getMCEVersion().get());
		attributes.putValue("Flint-Minecraft-Version", p.getMinecraftVersion().get());
		attributes.putValue("Flint-Tiny-Remapper-Version", p.getTinyRemapperVersion().get());
		attributes.putValue("Flint-Loader-Version", p.getFlintLoaderVersion().get());

		// This can be overridden by modules if required
		if (!attributes.containsKey("Flint-Mixin-Version")) {
			attributes.putValue("Flint-Mixin-Version", p.getMixinVersion().get().version());
			attributes.putValue("Flint-Mixin-Group", p.getMixinVersion().get().group());
		}

		for (Map.Entry<String, String> entry : extraValues.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue());
		}
	}

	private static Optional<String> getLoaderVersion(Project project) {
		SteelGradleExtension extension = SteelGradleExtension.get(project);

		if (extension.getInstallerData() == null) {
			project.getLogger().warn("Could not determine flint loader version for jar manifest");
			return Optional.empty();
		}

		return Optional.of(extension.getInstallerData().version());
	}

	private record MixinVersion(String group, String version) implements Serializable { }

	private static Optional<MixinVersion> getMixinVersion(Project project) {
		// Not super ideal that this uses the mod compile classpath, should prob look into making this not a thing at somepoint
		Optional<Dependency> dependency = project.getConfigurations().getByName(Constants.Configurations.LOADER_DEPENDENCIES)
				.getDependencies()
				.stream()
				.filter(dep -> "sponge-mixin".equals(dep.getName()))
				.findFirst();

		if (dependency.isEmpty()) {
			project.getLogger().warn("Could not determine Mixin version for jar manifest");
		}

		return dependency.map(d -> new MixinVersion(d.getGroup(), d.getVersion()));
	}
}
