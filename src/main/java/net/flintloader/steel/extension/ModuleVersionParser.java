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
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.JsonObject;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

import net.flintloader.steel.SteelGradlePlugin;

public class ModuleVersionParser {
	private final Project project;

	private String version = null;

	public ModuleVersionParser(Project project) {
		this.project = project;
	}

	public String getModVersion() {
		if (version != null) {
			return version;
		}

		File json = locateModJsonFile();
		JsonObject jsonObject;

		try (var reader = new FileReader(json)) {
			jsonObject = SteelGradlePlugin.GSON.fromJson(reader, JsonObject.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read flintmodule.json file");
		}

		if (!jsonObject.has("version") || !jsonObject.get("version").isJsonPrimitive()) {
			throw new UnsupportedOperationException("Could not find valid version in the flintmodule.json file");
		}

		version = jsonObject.get("version").getAsString();

		return version;
	}

	private File locateModJsonFile() {
		return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
				.getByName("main")
				.getResources()
				.matching(patternFilterable -> patternFilterable.include("flintmodule.json"))
				.getSingleFile();
	}
}