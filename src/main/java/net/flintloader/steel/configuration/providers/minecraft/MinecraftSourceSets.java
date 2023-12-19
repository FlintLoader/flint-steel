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

package net.flintloader.steel.configuration.providers.minecraft;

import static net.flintloader.steel.configuration.CompileConfiguration.extendsFrom;

import java.util.List;
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.configuration.CompileConfiguration;
import net.flintloader.steel.configuration.RemapConfigurations;
import net.flintloader.steel.task.AbstractRemapJarTask;
import net.flintloader.steel.util.Constants;
import net.flintloader.steel.util.gradle.SourceSetHelper;

public abstract sealed class MinecraftSourceSets permits MinecraftSourceSets.Single, MinecraftSourceSets.Split {
	public static MinecraftSourceSets get(Project project) {
		return SteelGradleExtension.get(project).areEnvironmentSourceSetsSplit() ? Split.INSTANCE : Single.INSTANCE;
	}

	public abstract void applyDependencies(BiConsumer<String, String> consumer, List<String> targets);

	public abstract String getCombinedSourceSetName();

	public abstract String getSourceSetForEnv(String env);

	protected abstract List<String> getAllSourceSetNames();

	public void evaluateSplit(Project project) {
		final SteelGradleExtension extension = SteelGradleExtension.get(project);
		Preconditions.checkArgument(extension.areEnvironmentSourceSetsSplit());

		Split.INSTANCE.evaluate(project);
	}

	public abstract void afterEvaluate(Project project);

	protected void createSourceSets(Project project) {
		for (String name : getAllSourceSetNames()) {
			project.getConfigurations().register(name, configuration -> configuration.setTransitive(false));

			// All the configurations extend the loader deps.
			extendsFrom(name, Constants.Configurations.LOADER_DEPENDENCIES, project);
		}
	}

	/**
	 * Used when we have a single source set, either with split or merged jars.
	 */
	public static final class Single extends MinecraftSourceSets {
		private static final String MINECRAFT_NAMED = "minecraftNamed";

		private static final Single INSTANCE = new Single();

		@Override
		public void applyDependencies(BiConsumer<String, String> consumer, List<String> targets) {
			for (String target : targets) {
				consumer.accept(MINECRAFT_NAMED, target);
			}
		}

		@Override
		public String getCombinedSourceSetName() {
			return MINECRAFT_NAMED;
		}

		@Override
		public String getSourceSetForEnv(String env) {
			return SourceSet.MAIN_SOURCE_SET_NAME;
		}

		@Override
		protected List<String> getAllSourceSetNames() {
			return List.of(MINECRAFT_NAMED);
		}

		@Override
		public void afterEvaluate(Project project) {
			// This is done in afterEvaluate as we need to be sure that split source sets was not enabled.
			createSourceSets(project);

			// Default compile and runtime sourcesets.
			CompileConfiguration.extendsFrom(List.of(
					JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME,
					JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME,
					JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME,
					JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME),
					MINECRAFT_NAMED, project
			);
		}
	}

	/**
	 * Used when we have a split client/common source set and split jars.
	 */
	public static final class Split extends MinecraftSourceSets {
		private static final String MINECRAFT_COMMON_NAMED = "minecraftCommonNamed";
		private static final String MINECRAFT_CLIENT_ONLY_NAMED = "minecraftClientOnlyNamed";
		private static final String MINECRAFT_COMBINED_NAMED = "minecraftCombinedNamed";

		private static final String CLIENT_ONLY_SOURCE_SET_NAME = "client";

		private static final Split INSTANCE = new Split();

		@Override
		public void applyDependencies(BiConsumer<String, String> consumer, List<String> targets) {
			Preconditions.checkArgument(targets.size() == 2);
			Preconditions.checkArgument(targets.contains("common"));
			Preconditions.checkArgument(targets.contains("clientOnly"));

			consumer.accept(MINECRAFT_COMMON_NAMED, "common");
			consumer.accept(MINECRAFT_CLIENT_ONLY_NAMED, "clientOnly");
		}

		@Override
		public String getCombinedSourceSetName() {
			return MINECRAFT_COMBINED_NAMED;
		}

		@Override
		public String getSourceSetForEnv(String env) {
			return env.equals("client") ? CLIENT_ONLY_SOURCE_SET_NAME : SourceSet.MAIN_SOURCE_SET_NAME;
		}

		@Override
		protected List<String> getAllSourceSetNames() {
			return List.of(MINECRAFT_COMMON_NAMED, MINECRAFT_CLIENT_ONLY_NAMED, MINECRAFT_COMBINED_NAMED);
		}

		// Called during evaluation, when the loom extension method is called.
		private void evaluate(Project project) {
			final SteelGradleExtension extension = SteelGradleExtension.get(project);
			createSourceSets(project);

			// Combined extends from the 2 environments.
			CompileConfiguration.extendsFrom(MINECRAFT_COMBINED_NAMED, MINECRAFT_COMMON_NAMED, project);
			CompileConfiguration.extendsFrom(MINECRAFT_COMBINED_NAMED, MINECRAFT_CLIENT_ONLY_NAMED, project);

			// Register our new client only source set, main becomes common only, with their respective jars.
			SourceSet mainSourceSet = SourceSetHelper.getMainSourceSet(project);
			SourceSet clientOnlySourceSet = SourceSetHelper.createSourceSet(CLIENT_ONLY_SOURCE_SET_NAME, project);

			CompileConfiguration.extendsFrom(List.of(
					mainSourceSet.getCompileClasspathConfigurationName(),
					mainSourceSet.getRuntimeClasspathConfigurationName()
				), MINECRAFT_COMMON_NAMED, project
			);

			CompileConfiguration.extendsFrom(List.of(
					clientOnlySourceSet.getCompileClasspathConfigurationName(),
					clientOnlySourceSet.getRuntimeClasspathConfigurationName()
				), MINECRAFT_CLIENT_ONLY_NAMED, project
			);

			// Client depends on common.
			CompileConfiguration.extendsFrom(MINECRAFT_CLIENT_ONLY_NAMED, MINECRAFT_COMMON_NAMED, project);
			clientOnlySourceSet.setCompileClasspath(
					clientOnlySourceSet.getCompileClasspath()
							.plus(mainSourceSet.getCompileClasspath())
							.plus(mainSourceSet.getOutput())
			);
			clientOnlySourceSet.setRuntimeClasspath(
					clientOnlySourceSet.getRuntimeClasspath()
							.plus(mainSourceSet.getRuntimeClasspath())
							.plus(mainSourceSet.getOutput())
			);

			RemapConfigurations.configureClientConfigurations(project, clientOnlySourceSet);

			// Include the client only output in the jars
			project.getTasks().named(mainSourceSet.getJarTaskName(), Jar.class).configure(jar -> {
				jar.from(clientOnlySourceSet.getOutput().getClassesDirs());
				jar.from(clientOnlySourceSet.getOutput().getResourcesDir());

				jar.dependsOn(project.getTasks().named(clientOnlySourceSet.getProcessResourcesTaskName()));
			});

			// Remap with the client compile classpath.
			project.getTasks().withType(AbstractRemapJarTask.class).configureEach(remapJarTask -> {
				remapJarTask.getClasspath().from(
						project.getConfigurations().getByName(clientOnlySourceSet.getCompileClasspathConfigurationName())
				);
			});

			// The sources task can be registered at a later time.
			project.getTasks().configureEach(task -> {
				if (!mainSourceSet.getSourcesJarTaskName().equals(task.getName()) || !(task instanceof Jar jar)) {
					// Not the sources task we are looking for.
					return;
				}

				// The client only sources to the combined sources jar.
				jar.from(clientOnlySourceSet.getAllSource());
			});

			extension.getInterfaceInjection().getInterfaceInjectionSourceSets().add(clientOnlySourceSet);
		}

		@Override
		public void afterEvaluate(Project project) {
		}

		public static SourceSet getClientSourceSet(Project project) {
			Preconditions.checkArgument(SteelGradleExtension.get(project).areEnvironmentSourceSetsSplit(), "Cannot get client only sourceset as project is not split");
			return SourceSetHelper.getSourceSetByName(CLIENT_ONLY_SOURCE_SET_NAME, project);
		}
	}
}
