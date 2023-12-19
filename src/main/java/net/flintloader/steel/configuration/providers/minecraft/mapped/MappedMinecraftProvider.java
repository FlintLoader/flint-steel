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

package net.flintloader.steel.configuration.providers.minecraft.mapped;

import java.nio.file.Path;
import java.util.List;

public interface MappedMinecraftProvider {
	List<Path> getMinecraftJars();

	interface ProviderImpl extends MappedMinecraftProvider {
		Path getJar(String name);
	}

	interface Merged extends ProviderImpl {
		String MERGED = "merged";

		default Path getMergedJar() {
			return getJar(MERGED);
		}

		@Override
		default List<Path> getMinecraftJars() {
			return List.of(getMergedJar());
		}
	}

	interface Split extends ProviderImpl {
		String COMMON = "common";
		String CLIENT_ONLY = "clientOnly";

		default Path getCommonJar() {
			return getJar(COMMON);
		}

		default Path getClientOnlyJar() {
			return getJar(CLIENT_ONLY);
		}

		@Override
		default List<Path> getMinecraftJars() {
			return List.of(getCommonJar(), getClientOnlyJar());
		}
	}

	interface SingleJar extends ProviderImpl {
		String env();

		default String envName() {
			return "%sOnly".formatted(env());
		}

		default Path getEnvOnlyJar() {
			return getJar(envName());
		}

		@Override
		default List<Path> getMinecraftJars() {
			return List.of(getEnvOnlyJar());
		}
	}
}
