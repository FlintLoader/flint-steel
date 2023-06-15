/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2019-2021 HypherionSA and Contributors
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

package net.flintloader.steel.decompilers.fernflower;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import net.flintloader.steel.api.decompilers.DecompilationMetadata;
import net.flintloader.steel.api.decompilers.SteelDecompiler;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;

public final class FabricFernFlowerDecompiler implements SteelDecompiler {
	@Override
	public void decompile(Path compiledJar, Path sourcesDestination, Path linemapDestination, DecompilationMetadata metaData) {
		final Map<String, Object> options = new HashMap<>(
				Map.of(
					IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1",
					IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",
					IFernflowerPreferences.REMOVE_SYNTHETIC, "1",
					IFernflowerPreferences.LOG_LEVEL, "trace",
					IFernflowerPreferences.THREADS, String.valueOf(metaData.numberOfThreads()),
					IFernflowerPreferences.INDENT_STRING, "\t",
					IFabricJavadocProvider.PROPERTY_NAME, new TinyJavadocProvider(metaData.javaDocs().toFile())
				)
		);

		options.putAll(metaData.options());

		IResultSaver saver = new ThreadSafeResultSaver(sourcesDestination::toFile, linemapDestination::toFile);
		Fernflower ff = new Fernflower(FernFlowerUtils::getBytecode, saver, options, new FernflowerLogger(metaData.logger()));

		for (Path library : metaData.libraries()) {
			ff.addLibrary(library.toFile());
		}

		ff.addSource(compiledJar.toFile());

		try {
			ff.decompileContext();
		} finally {
			ff.clearContext();
		}
	}
}
