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

package net.flintloader.steel.api.mappings.layered;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.ApiStatus;

import net.flintloader.steel.configuration.providers.minecraft.MinecraftProvider;
import net.flintloader.steel.util.download.DownloadBuilder;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

@ApiStatus.Experimental /* Very Experimental and not cleanly separated from the impl atm */
public interface MappingContext {
	Path resolveDependency(Dependency dependency);

	Path resolveMavenDependency(String mavenNotation);

	Supplier<MemoryMappingTree> intermediaryTree();

	MinecraftProvider minecraftProvider();

	default String minecraftVersion() {
		return minecraftProvider().minecraftVersion();
	}

	/**
	 * Creates a temporary working dir to be used to store working files.
	 */
	Path workingDirectory(String name);

	Logger getLogger();

	DownloadBuilder download(String url);

	boolean refreshDeps();
}
