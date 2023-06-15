/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2021 HypherionSA and Contributors
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

import org.gradle.api.Project;

import net.flintloader.steel.SteelGradleExtension;

public abstract class SteelFilesBaseImpl implements SteelFiles {
	protected abstract File getGradleUserHomeDir();
	protected abstract File getRootDir();
	protected abstract File getProjectDir();
	protected abstract File getBuildDir();

	public SteelFilesBaseImpl() { }

	private static File createFile(File parent, String child) {
		File file = new File(parent, child);

		if (!file.exists()) {
			file.mkdirs();
		}

		return file;
	}

	@Override
	public File getUserCache() {
		return createFile(getGradleUserHomeDir(), "caches" + File.separator + "flint-steel");
	}

	@Override
	public File getRootProjectPersistentCache() {
		return createFile(getRootDir(), ".gradle" + File.separator + "steel-cache");
	}

	@Override
	public File getProjectPersistentCache() {
		return createFile(getProjectDir(), ".gradle" + File.separator + "steel-cache");
	}

	@Override
	public File getProjectBuildCache() {
		return createFile(getBuildDir(), "steel-cache");
	}

	@Override
	public File getRemappedModCache() {
		return createFile(getRootProjectPersistentCache(), "remapped_modules");
	}

	@Override
	public File getNativesDirectory(Project project) {
		return createFile(getRootProjectPersistentCache(), "natives/" + SteelGradleExtension.get(project).getMinecraftProvider().minecraftVersion());
	}

	@Override
	public File getDefaultLog4jConfigFile() {
		return new File(getProjectPersistentCache(), "log4j.xml");
	}

	@Override
	public File getDevLauncherConfig() {
		return new File(getProjectPersistentCache(), "launch.cfg");
	}

	@Override
	public File getUnpickLoggingConfigFile() {
		return new File(getProjectPersistentCache(), "unpick-logging.properties");
	}

	@Override
	public File getRemapClasspathFile() {
		return new File(getProjectPersistentCache(), "remapClasspath.txt");
	}
}
