/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2018-2021 HypherionSA and Contributors
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

package net.flintloader.steel.configuration.providers.mappings.parchment;

import net.flintloader.steel.api.mappings.layered.MappingContext;
import net.flintloader.steel.api.mappings.layered.spec.FileSpec;
import net.flintloader.steel.api.mappings.layered.spec.MappingsSpec;

public record ParchmentMappingsSpec(FileSpec fileSpec, boolean removePrefix) implements MappingsSpec<ParchmentMappingLayer> {
	@Override
	public ParchmentMappingLayer createLayer(MappingContext context) {
		return new ParchmentMappingLayer(fileSpec.get(context), removePrefix());
	}
}
