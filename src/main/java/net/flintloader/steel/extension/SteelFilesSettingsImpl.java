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
import java.util.Objects;

import org.gradle.api.initialization.Settings;

public class SteelFilesSettingsImpl extends SteelFilesBaseImpl {
	private final Settings settings;

	public SteelFilesSettingsImpl(Settings settings) {
		this.settings = Objects.requireNonNull(settings);
	}

	@Override
	protected File getGradleUserHomeDir() {
		return settings.getGradle().getGradleUserHomeDir();
	}

	@Override
	protected File getRootDir() {
		return settings.getRootDir();
	}

	@Override
	protected File getProjectDir() {
		throw new IllegalStateException("You can not access project directory from setting stage");
	}

	@Override
	protected File getBuildDir() {
		throw new IllegalStateException("You can not access project build directory from setting stage");
	}
}