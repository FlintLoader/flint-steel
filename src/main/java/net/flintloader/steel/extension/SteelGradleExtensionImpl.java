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

package net.flintloader.steel.extension;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.api.mappings.intermediate.IntermediateMappingsProvider;
import net.flintloader.steel.api.mappings.layered.MappingsNamespace;
import net.flintloader.steel.configuration.InstallerData;
import net.flintloader.steel.configuration.SteelDependencyManager;
import net.flintloader.steel.configuration.accesswidener.AccessWidenerFile;
import net.flintloader.steel.configuration.processors.JarProcessorManager;
import net.flintloader.steel.configuration.providers.mappings.IntermediaryMappingsProvider;
import net.flintloader.steel.configuration.providers.mappings.MappingsProviderImpl;
import net.flintloader.steel.configuration.providers.minecraft.MinecraftProvider;
import net.flintloader.steel.configuration.providers.minecraft.mapped.IntermediaryMinecraftProvider;
import net.flintloader.steel.configuration.providers.minecraft.mapped.NamedMinecraftProvider;
import net.flintloader.steel.util.download.Download;
import net.flintloader.steel.util.download.DownloadBuilder;

public class SteelGradleExtensionImpl extends SteelGradleExtensionApiImpl implements SteelGradleExtension {
	private final Project project;
	private final MixinExtension mixinApExtension;
	private final SteelFiles steelFiles;
	private final ConfigurableFileCollection unmappedModules;

	private final MappingSet[] srcMappingCache = new MappingSet[2];
	private final Mercury[] srcMercuryCache = new Mercury[2];
	private final List<AccessWidenerFile> transitiveAccessWideners = new ArrayList<>();

	private SteelDependencyManager dependencyManager;
	private JarProcessorManager jarProcessorManager;
	private MinecraftProvider minecraftProvider;
	private MappingsProviderImpl mappingsProvider;
	private NamedMinecraftProvider<?> namedMinecraftProvider;
	private IntermediaryMinecraftProvider<?> intermediaryMinecraftProvider;
	private InstallerData installerData;
	private boolean refreshDeps;

	public SteelGradleExtensionImpl(Project project, SteelFiles files) {
		super(project, files);
		this.project = project;
		// Initiate with newInstance to allow gradle to decorate our extension
		this.mixinApExtension = project.getObjects().newInstance(MixinExtensionImpl.class, project);
		this.steelFiles = files;
		this.unmappedModules = project.files();

		// Setup the default intermediate mappings provider.
		setIntermediateMappingsProvider(IntermediaryMappingsProvider.class, provider -> {
			provider.getIntermediaryUrl()
					.convention(getIntermediaryUrl())
					.finalizeValueOnRead();

			provider.getRefreshDeps().set(project.provider(() -> SteelGradleExtension.get(project).refreshDeps()));
		});

		refreshDeps = project.getGradle().getStartParameter().isRefreshDependencies() || Boolean.getBoolean("steel.refresh");

		if (refreshDeps) {
			project.getLogger().lifecycle("Refresh dependencies is in use, steel will be significantly slower.");
		}
	}

	@Override
	protected Project getProject() {
		return project;
	}

	@Override
	public SteelFiles getFiles() {
		return steelFiles;
	}

	@Override
	public void setDependencyManager(SteelDependencyManager dependencyManager) {
		this.dependencyManager = dependencyManager;
	}

	@Override
	public SteelDependencyManager getDependencyManager() {
		return Objects.requireNonNull(dependencyManager, "Cannot get SteelDependencyManager before it has been setup");
	}

	@Override
	public void setJarProcessorManager(JarProcessorManager jarProcessorManager) {
		this.jarProcessorManager = jarProcessorManager;
	}

	@Override
	public JarProcessorManager getJarProcessorManager() {
		return Objects.requireNonNull(jarProcessorManager, "Cannot get JarProcessorManager before it has been setup");
	}

	@Override
	public MinecraftProvider getMinecraftProvider() {
		return Objects.requireNonNull(minecraftProvider, "Cannot get MinecraftProvider before it has been setup");
	}

	@Override
	public void setMinecraftProvider(MinecraftProvider minecraftProvider) {
		this.minecraftProvider = minecraftProvider;
	}

	@Override
	public MappingsProviderImpl getMappingsProvider() {
		return Objects.requireNonNull(mappingsProvider, "Cannot get MappingsProvider before it has been setup");
	}

	@Override
	public void setMappingsProvider(MappingsProviderImpl mappingsProvider) {
		this.mappingsProvider = mappingsProvider;
	}

	@Override
	public NamedMinecraftProvider<?> getNamedMinecraftProvider() {
		return Objects.requireNonNull(namedMinecraftProvider, "Cannot get NamedMinecraftProvider before it has been setup");
	}

	@Override
	public IntermediaryMinecraftProvider<?> getIntermediaryMinecraftProvider() {
		return Objects.requireNonNull(intermediaryMinecraftProvider, "Cannot get IntermediaryMinecraftProvider before it has been setup");
	}

	@Override
	public void setNamedMinecraftProvider(NamedMinecraftProvider<?> namedMinecraftProvider) {
		this.namedMinecraftProvider = namedMinecraftProvider;
	}

	@Override
	public void setIntermediaryMinecraftProvider(IntermediaryMinecraftProvider<?> intermediaryMinecraftProvider) {
		this.intermediaryMinecraftProvider = intermediaryMinecraftProvider;
	}

	@Override
	public FileCollection getMinecraftJarsCollection(MappingsNamespace mappingsNamespace) {
		return getProject().files(
			getProject().provider(() ->
				getProject().files(getMinecraftJars(mappingsNamespace).stream().map(Path::toFile).toList())
			)
		);
	}

	@Override
	public MappingSet getOrCreateSrcMappingCache(int id, Supplier<MappingSet> factory) {
		return srcMappingCache[id] != null ? srcMappingCache[id] : (srcMappingCache[id] = factory.get());
	}

	@Override
	public Mercury getOrCreateSrcMercuryCache(int id, Supplier<Mercury> factory) {
		return srcMercuryCache[id] != null ? srcMercuryCache[id] : (srcMercuryCache[id] = factory.get());
	}

	@Override
	public ConfigurableFileCollection getUnmappedModCollection() {
		return unmappedModules;
	}

	public void setInstallerData(InstallerData object) {
		this.installerData = object;
	}

	@Override
	public InstallerData getInstallerData() {
		return installerData;
	}

	@Override
	public boolean isRootProject() {
		return project.getRootProject() == project;
	}

	@Override
	public MixinExtension getMixin() {
		return this.mixinApExtension;
	}

	@Override
	public List<AccessWidenerFile> getTransitiveAccessWideners() {
		return transitiveAccessWideners;
	}

	@Override
	public void addTransitiveAccessWideners(List<AccessWidenerFile> accessWidenerFiles) {
		transitiveAccessWideners.addAll(accessWidenerFiles);
	}

	@Override
	public DownloadBuilder download(String url) {
		DownloadBuilder builder;

		try {
			builder = Download.create(url);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to create downloader for: " + e);
		}

		if (project.getGradle().getStartParameter().isOffline()) {
			builder.offline();
		}

		if (refreshDeps()) {
			builder.forceDownload();
		}

		return builder;
	}

	@Override
	public boolean refreshDeps() {
		return refreshDeps;
	}

	@Override
	public void setRefreshDeps(boolean refreshDeps) {
		this.refreshDeps = refreshDeps;
	}

	@Override
	protected <T extends IntermediateMappingsProvider> void configureIntermediateMappingsProviderInternal(T provider) {
		provider.getMinecraftVersion().set(getProject().provider(() -> getMinecraftProvider().minecraftVersion()));
		provider.getMinecraftVersion().disallowChanges();

		provider.getDownloader().set(this::download);
		provider.getDownloader().disallowChanges();
	}
}
