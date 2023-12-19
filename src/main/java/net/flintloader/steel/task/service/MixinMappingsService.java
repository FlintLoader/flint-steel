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

package net.flintloader.steel.task.service;

import java.io.File;
import java.util.HashSet;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.configuration.providers.mappings.MappingsProviderImpl;
import net.flintloader.steel.util.service.SharedService;
import net.flintloader.steel.util.service.SharedServiceManager;

import net.fabricmc.tinyremapper.IMappingProvider;

public final class MixinMappingsService implements SharedService {
	private final SharedServiceManager sharedServiceManager;
	private final HashSet<File> mixinMappings = new HashSet<>();

	private MixinMappingsService(SharedServiceManager sharedServiceManager) {
		this.sharedServiceManager = sharedServiceManager;
	}

	public static File getMixinMappingFile(Project project, SourceSet sourceSet) {
		final SteelGradleExtension extension = SteelGradleExtension.get(project);
		File mixinMapping = new File(extension.getFiles().getProjectBuildCache(), "mixin-map-" + extension.getMappingsProvider().mappingsIdentifier() + "." + sourceSet.getName() + ".tiny");

		getService(SharedServiceManager.get(project), extension.getMappingsProvider()).mixinMappings.add(mixinMapping);

		return mixinMapping;
	}

	static synchronized MixinMappingsService getService(SharedServiceManager sharedServiceManager, MappingsProviderImpl mappingsProvider) {
		return sharedServiceManager.getOrCreateService("MixinMappings-" + mappingsProvider.mappingsIdentifier(), () -> new MixinMappingsService(sharedServiceManager));
	}

	IMappingProvider getMappingProvider(String from, String to) {
		return out -> {
			for (File mixinMapping : mixinMappings) {
				if (!mixinMapping.exists()) continue;

				MappingsService service = MappingsService.create(sharedServiceManager, mixinMapping.getAbsolutePath(), mixinMapping.toPath(), from, to, false);
				service.getMappingsProvider().load(out);
			}
		};
	}
}
