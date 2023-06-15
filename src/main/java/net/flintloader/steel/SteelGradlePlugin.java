/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2016-2022 HypherionSA and Contributors
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

import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginAware;

import net.flintloader.steel.api.SteelGradleExtensionAPI;
import net.flintloader.steel.bootstrap.BootstrappedPlugin;
import net.flintloader.steel.configuration.CompileConfiguration;
import net.flintloader.steel.configuration.MavenPublication;
import net.flintloader.steel.configuration.ide.IdeConfiguration;
import net.flintloader.steel.configuration.ide.idea.IdeaConfiguration;
import net.flintloader.steel.decompilers.DecompilerConfiguration;
import net.flintloader.steel.extension.SteelFiles;
import net.flintloader.steel.extension.SteelGradleExtensionImpl;
import net.flintloader.steel.task.SteelTasks;
import net.flintloader.steel.util.LibraryLocationLogger;

public class SteelGradlePlugin implements BootstrappedPlugin {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	public static final String STEEL_VERSION = Objects.requireNonNullElse(SteelGradlePlugin.class.getPackage().getImplementationVersion(), "0.0.0+unknown");

	@Override
	public void apply(PluginAware target) {
		target.getPlugins().apply(SteelRepositoryPlugin.class);

		if (target instanceof Project project) {
			apply(project);
		}
	}

	public void apply(Project project) {
		project.getLogger().lifecycle("Flint Steel: " + STEEL_VERSION);
		LibraryLocationLogger.logLibraryVersions();

		// Apply default plugins
		project.apply(ImmutableMap.of("plugin", "java-library"));
		project.apply(ImmutableMap.of("plugin", "eclipse"));
		project.apply(ImmutableMap.of("plugin", "idea"));

		// Setup extensions
		project.getExtensions().create(SteelGradleExtensionAPI.class, "steel", SteelGradleExtensionImpl.class, project, SteelFiles.create(project));

		CompileConfiguration.setupConfigurations(project);
		IdeConfiguration.setup(project);
		CompileConfiguration.configureCompile(project);
		MavenPublication.configure(project);
		SteelTasks.registerTasks(project);
		DecompilerConfiguration.setup(project);
		IdeaConfiguration.setup(project);
	}
}
