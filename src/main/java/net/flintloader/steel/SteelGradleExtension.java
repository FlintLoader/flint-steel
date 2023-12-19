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

package net.flintloader.steel;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;

import net.flintloader.steel.api.SteelGradleExtensionAPI;
import net.flintloader.steel.api.mappings.layered.MappingsNamespace;
import net.flintloader.steel.configuration.InstallerData;
import net.flintloader.steel.configuration.SteelDependencyManager;
import net.flintloader.steel.configuration.accesswidener.AccessWidenerFile;
import net.flintloader.steel.configuration.processors.JarProcessorManager;
import net.flintloader.steel.configuration.providers.mappings.MappingsProviderImpl;
import net.flintloader.steel.configuration.providers.minecraft.MinecraftProvider;
import net.flintloader.steel.configuration.providers.minecraft.mapped.IntermediaryMinecraftProvider;
import net.flintloader.steel.configuration.providers.minecraft.mapped.NamedMinecraftProvider;
import net.flintloader.steel.extension.MixinExtension;
import net.flintloader.steel.extension.SteelFiles;
import net.flintloader.steel.util.download.DownloadBuilder;

public interface SteelGradleExtension extends SteelGradleExtensionAPI {
	static SteelGradleExtension get(Project project) {
		return (SteelGradleExtension) project.getExtensions().getByName("steel");
	}

	SteelFiles getFiles();

	MappingSet getOrCreateSrcMappingCache(int id, Supplier<MappingSet> factory);

	Mercury getOrCreateSrcMercuryCache(int id, Supplier<Mercury> factory);

	ConfigurableFileCollection getUnmappedModCollection();

	void setInstallerData(InstallerData data);

	InstallerData getInstallerData();

	void setDependencyManager(SteelDependencyManager dependencyManager);

	SteelDependencyManager getDependencyManager();

	void setJarProcessorManager(JarProcessorManager jarProcessorManager);

	JarProcessorManager getJarProcessorManager();

	MinecraftProvider getMinecraftProvider();

	void setMinecraftProvider(MinecraftProvider minecraftProvider);

	MappingsProviderImpl getMappingsProvider();

	void setMappingsProvider(MappingsProviderImpl mappingsProvider);

	NamedMinecraftProvider<?> getNamedMinecraftProvider();

	IntermediaryMinecraftProvider<?> getIntermediaryMinecraftProvider();

	void setNamedMinecraftProvider(NamedMinecraftProvider<?> namedMinecraftProvider);

	void setIntermediaryMinecraftProvider(IntermediaryMinecraftProvider<?> intermediaryMinecraftProvider);

	default List<Path> getMinecraftJars(MappingsNamespace mappingsNamespace) {
		return switch (mappingsNamespace) {
		case NAMED -> getNamedMinecraftProvider().getMinecraftJars();
		case INTERMEDIARY -> getIntermediaryMinecraftProvider().getMinecraftJars();
		case OFFICIAL -> getMinecraftProvider().getMinecraftJars();
		};
	}

	FileCollection getMinecraftJarsCollection(MappingsNamespace mappingsNamespace);

	boolean isRootProject();

	@Override
	MixinExtension getMixin();

	List<AccessWidenerFile> getTransitiveAccessWideners();

	void addTransitiveAccessWideners(List<AccessWidenerFile> accessWidenerFiles);

	DownloadBuilder download(String url);

	boolean refreshDeps();

	void setRefreshDeps(boolean refreshDeps);
}
