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

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.api.InterfaceInjectionExtensionAPI;
import net.flintloader.steel.api.MixinExtensionAPI;
import net.flintloader.steel.api.ModuleSettings;
import net.flintloader.steel.api.RemapConfigurationSettings;
import net.flintloader.steel.api.SteelGradleExtensionAPI;
import net.flintloader.steel.api.decompilers.DecompilerOptions;
import net.flintloader.steel.api.mappings.intermediate.IntermediateMappingsProvider;
import net.flintloader.steel.api.mappings.layered.spec.LayeredMappingSpecBuilder;
import net.flintloader.steel.configuration.RemapConfigurations;
import net.flintloader.steel.configuration.ide.RunConfigSettings;
import net.flintloader.steel.configuration.processors.JarProcessor;
import net.flintloader.steel.configuration.providers.mappings.GradleMappingContext;
import net.flintloader.steel.configuration.providers.mappings.LayeredMappingSpec;
import net.flintloader.steel.configuration.providers.mappings.LayeredMappingSpecBuilderImpl;
import net.flintloader.steel.configuration.providers.mappings.LayeredMappingsDependency;
import net.flintloader.steel.configuration.providers.minecraft.MinecraftJarConfiguration;
import net.flintloader.steel.configuration.providers.minecraft.MinecraftSourceSets;
import net.flintloader.steel.task.GenerateSourcesTask;
import net.flintloader.steel.util.DeprecationHelper;
import net.flintloader.steel.util.gradle.SourceSetHelper;

/**
 * This class implements the public extension api.
 */
public abstract class SteelGradleExtensionApiImpl implements SteelGradleExtensionAPI {
	protected final DeprecationHelper deprecationHelper;
	protected final ListProperty<JarProcessor> jarProcessors;
	protected final ConfigurableFileCollection log4jConfigs;
	protected final RegularFileProperty accessWidener;
	protected final Property<String> customManifest;
	protected final Property<Boolean> transitiveAccessWideners;
	protected final Property<Boolean> modProvidedJavadoc;
	protected final Property<String> intermediary;
	protected final Property<IntermediateMappingsProvider> intermediateMappingsProvider;
	private final Property<Boolean> runtimeOnlyLog4j;
	private final Property<Boolean> splitModDependencies;
	private final Property<MinecraftJarConfiguration> minecraftJarConfiguration;
	private final Property<Boolean> splitEnvironmentalSourceSet;
	private final InterfaceInjectionExtensionAPI interfaceInjectionExtension;

	private final ModuleVersionParser versionParser;

	private final NamedDomainObjectContainer<RunConfigSettings> runConfigs;
	private final NamedDomainObjectContainer<DecompilerOptions> decompilers;
	private final NamedDomainObjectContainer<ModuleSettings> mods;
	private final NamedDomainObjectList<RemapConfigurationSettings> remapConfigurations;

	// A common mistake with layered mappings is to call the wrong `officialMojangMappings` method, use this to keep track of when we are building a layered mapping spec.
	protected final ThreadLocal<Boolean> layeredSpecBuilderScope = ThreadLocal.withInitial(() -> false);

	protected SteelGradleExtensionApiImpl(Project project, SteelFiles directories) {
		this.jarProcessors = project.getObjects().listProperty(JarProcessor.class)
				.empty();
		this.log4jConfigs = project.files(directories.getDefaultLog4jConfigFile());
		this.accessWidener = project.getObjects().fileProperty();
		this.customManifest = project.getObjects().property(String.class);
		this.transitiveAccessWideners = project.getObjects().property(Boolean.class)
				.convention(true);
		this.transitiveAccessWideners.finalizeValueOnRead();
		this.modProvidedJavadoc = project.getObjects().property(Boolean.class)
				.convention(true);
		this.modProvidedJavadoc.finalizeValueOnRead();
		this.intermediary = project.getObjects().property(String.class)
				.convention("https://maven.flintloader.net/mirror/net/fabricmc/intermediary/%1$s/intermediary-%1$s-v2.jar");

		this.intermediateMappingsProvider = project.getObjects().property(IntermediateMappingsProvider.class);
		this.intermediateMappingsProvider.finalizeValueOnRead();

		this.versionParser = new ModuleVersionParser(project);

		this.deprecationHelper = new DeprecationHelper.ProjectBased(project);

		this.runConfigs = project.container(RunConfigSettings.class,
				baseName -> new RunConfigSettings(project, baseName));
		this.decompilers = project.getObjects().domainObjectContainer(DecompilerOptions.class);
		this.mods = project.getObjects().domainObjectContainer(ModuleSettings.class);
		this.remapConfigurations = project.getObjects().namedDomainObjectList(RemapConfigurationSettings.class);

		this.minecraftJarConfiguration = project.getObjects().property(MinecraftJarConfiguration.class).convention(MinecraftJarConfiguration.CLIENT_ONLY);
		this.minecraftJarConfiguration.finalizeValueOnRead();

		this.accessWidener.finalizeValueOnRead();
		this.getGameJarProcessors().finalizeValueOnRead();

		this.runtimeOnlyLog4j = project.getObjects().property(Boolean.class).convention(false);
		this.runtimeOnlyLog4j.finalizeValueOnRead();

		this.splitModDependencies = project.getObjects().property(Boolean.class).convention(true);
		this.splitModDependencies.finalizeValueOnRead();

		this.interfaceInjectionExtension = project.getObjects().newInstance(InterfaceInjectionExtensionAPI.class);

		this.splitEnvironmentalSourceSet = project.getObjects().property(Boolean.class).convention(false);
		this.splitEnvironmentalSourceSet.finalizeValueOnRead();

		// Add main source set by default
		interfaceInjection(interfaceInjection -> {
			final SourceSet main = SourceSetHelper.getMainSourceSet(project);
			interfaceInjection.getInterfaceInjectionSourceSets().add(main);

			interfaceInjection.getInterfaceInjectionSourceSets().finalizeValueOnRead();
			interfaceInjection.getEnableDependencyInterfaceInjection().convention(true).finalizeValueOnRead();
		});
	}

	@Override
	public DeprecationHelper getDeprecationHelper() {
		return deprecationHelper;
	}

	@Override
	public RegularFileProperty getAccessWidenerPath() {
		return accessWidener;
	}

	@Override
	public NamedDomainObjectContainer<DecompilerOptions> getDecompilerOptions() {
		return decompilers;
	}

	@Override
	public void decompilers(Action<NamedDomainObjectContainer<DecompilerOptions>> action) {
		action.execute(decompilers);
	}

	@Override
	public ListProperty<JarProcessor> getGameJarProcessors() {
		return jarProcessors;
	}

	@Override
	public Dependency officialMojangMappings() {
		if (layeredSpecBuilderScope.get()) {
			throw new IllegalStateException("Use `officialMojangMappings()` when configuring layered mappings, not the extension method `steel.officialMojangMappings()`");
		}

		return layered(LayeredMappingSpecBuilder::officialMojangMappings);
	}

	@Override
	public Dependency layered(Action<LayeredMappingSpecBuilder> action) {
		LayeredMappingSpecBuilderImpl builder = new LayeredMappingSpecBuilderImpl();

		layeredSpecBuilderScope.set(true);
		action.execute(builder);
		layeredSpecBuilderScope.set(false);

		LayeredMappingSpec builtSpec = builder.build();
		return new LayeredMappingsDependency(getProject(), new GradleMappingContext(getProject(), builtSpec.getVersion().replace("+", "_").replace(".", "_")), builtSpec, builtSpec.getVersion());
	}

	@Override
	public void runs(Action<NamedDomainObjectContainer<RunConfigSettings>> action) {
		action.execute(runConfigs);
	}

	@Override
	public NamedDomainObjectContainer<RunConfigSettings> getRunConfigs() {
		return runConfigs;
	}

	@Override
	public ConfigurableFileCollection getLog4jConfigs() {
		return log4jConfigs;
	}

	@Override
	public void mixin(Action<MixinExtensionAPI> action) {
		action.execute(getMixin());
	}

	@Override
	public Property<String> getCustomMinecraftManifest() {
		return customManifest;
	}

	@Override
	public String getModuleVersion() {
		return versionParser.getModuleVersion();
	}

	@Override
	public Property<Boolean> getEnableTransitiveAccessWideners() {
		return transitiveAccessWideners;
	}

	@Override
	public Property<Boolean> getEnableModuleProvidedJavadoc() {
		return modProvidedJavadoc;
	}

	protected abstract Project getProject();

	protected abstract SteelFiles getFiles();

	@Override
	public Property<String> getIntermediaryUrl() {
		return intermediary;
	}

	@Override
	public IntermediateMappingsProvider getIntermediateMappingsProvider() {
		return intermediateMappingsProvider.get();
	}

	@Override
	public void setIntermediateMappingsProvider(IntermediateMappingsProvider intermediateMappingsProvider) {
		this.intermediateMappingsProvider.set(intermediateMappingsProvider);
	}

	@Override
	public <T extends IntermediateMappingsProvider> void setIntermediateMappingsProvider(Class<T> clazz, Action<T> action) {
		T provider = getProject().getObjects().newInstance(clazz);
		configureIntermediateMappingsProviderInternal(provider);
		action.execute(provider);
		intermediateMappingsProvider.set(provider);
	}

	@Override
	public File getMappingsFile() {
		return SteelGradleExtension.get(getProject()).getMappingsProvider().tinyMappings.toFile();
	}

	@Override
	public GenerateSourcesTask getDecompileTask(DecompilerOptions options, boolean client) {
		final String decompilerName = options.getFormattedName();
		final String taskName;

		if (areEnvironmentSourceSetsSplit()) {
			taskName = "gen%sSourcesWith%s".formatted(client ? "ClientOnly" : "Common", decompilerName);
		} else {
			taskName = "genSourcesWith" + decompilerName;
		}

		return (GenerateSourcesTask) getProject().getTasks().getByName(taskName);
	}

	protected abstract <T extends IntermediateMappingsProvider> void configureIntermediateMappingsProviderInternal(T provider);

	@Override
	public void disableDeprecatedPomGeneration(MavenPublication publication) {
		net.flintloader.steel.configuration.MavenPublication.excludePublication(publication);
	}

	@Override
	public Property<MinecraftJarConfiguration> getMinecraftJarConfiguration() {
		return minecraftJarConfiguration;
	}

	@Override
	public Property<Boolean> getRuntimeOnlyLog4j() {
		return runtimeOnlyLog4j;
	}

	@Override
	public Property<Boolean> getSplitModuleDependencies() {
		return splitModDependencies;
	}

	@Override
	public void splitEnvironmentSourceSets() {
		splitEnvironmentalSourceSet.set(true);

		// We need to lock these values, as we setup the new source sets right away.
		splitEnvironmentalSourceSet.finalizeValue();
		minecraftJarConfiguration.finalizeValue();

		MinecraftSourceSets.get(getProject()).evaluateSplit(getProject());
	}

	@Override
	public boolean areEnvironmentSourceSetsSplit() {
		return splitEnvironmentalSourceSet.get();
	}

	@Override
	public InterfaceInjectionExtensionAPI getInterfaceInjection() {
		return interfaceInjectionExtension;
	}

	@Override
	public void modules(Action<NamedDomainObjectContainer<ModuleSettings>> action) {
		action.execute(getModules());
	}

	@Override
	public NamedDomainObjectContainer<ModuleSettings> getModules() {
		return mods;
	}

	@Override
	public NamedDomainObjectList<RemapConfigurationSettings> getRemapConfigurations() {
		return remapConfigurations;
	}

	@Override
	public RemapConfigurationSettings addRemapConfiguration(String name, Action<RemapConfigurationSettings> action) {
		final RemapConfigurationSettings configurationSettings = getProject().getObjects().newInstance(RemapConfigurationSettings.class, name);

		// TODO remove in 2.0, this is a fallback to mimic the previous (Broken) behaviour
		configurationSettings.getSourceSet().convention(SourceSetHelper.getMainSourceSet(getProject()));

		action.execute(configurationSettings);
		RemapConfigurations.applyToProject(getProject(), configurationSettings);
		remapConfigurations.add(configurationSettings);

		return configurationSettings;
	}

	@Override
	public void createRemapConfigurations(SourceSet sourceSet) {
		RemapConfigurations.setupForSourceSet(getProject(), sourceSet);
	}

	// This is here to ensure that SteelGradleExtensionApiImpl compiles without any unimplemented methods
	private final class EnsureCompile extends SteelGradleExtensionApiImpl {
		private EnsureCompile() {
			super(null, null);
			throw new RuntimeException();
		}

		@Override
		public DeprecationHelper getDeprecationHelper() {
			throw new RuntimeException("Yeah... something is really wrong");
		}

		@Override
		protected Project getProject() {
			throw new RuntimeException("Yeah... something is really wrong");
		}

		@Override
		protected SteelFiles getFiles() {
			throw new RuntimeException("Yeah... something is really wrong");
		}

		@Override
		protected <T extends IntermediateMappingsProvider> void configureIntermediateMappingsProviderInternal(T provider) {
			throw new RuntimeException("Yeah... something is really wrong");
		}

		@Override
		public MixinExtension getMixin() {
			throw new RuntimeException("Yeah... something is really wrong");
		}
	}
}
