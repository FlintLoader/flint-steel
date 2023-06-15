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

package net.flintloader.steel.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.api.mappings.layered.MappingsNamespace;
import net.flintloader.steel.extension.SteelFiles;
import net.flintloader.steel.util.Constants;

public abstract class UnpickJarTask extends JavaExec {
	@InputFile
	public abstract RegularFileProperty getInputJar();

	@InputFile
	public abstract RegularFileProperty getUnpickDefinitions();

	@InputFiles
	// Only 1 file, but it comes from a configuration
	public abstract ConfigurableFileCollection getConstantJar();

	@InputFiles
	public abstract ConfigurableFileCollection getUnpickClasspath();

	@OutputFile
	public abstract RegularFileProperty getOutputJar();

	@Inject
	public UnpickJarTask() {
		classpath(getProject().getConfigurations().getByName(Constants.Configurations.UNPICK_CLASSPATH));
		getMainClass().set("daomephsta.unpick.cli.Main");

		getConstantJar().setFrom(getProject().getConfigurations().getByName(Constants.Configurations.MAPPING_CONSTANTS));
		getUnpickClasspath().setFrom(getProject().getConfigurations().getByName(Constants.Configurations.MINECRAFT_DEPENDENCIES));
	}

	@Override
	public void exec() {
		fileArg(getInputJar().get().getAsFile(), getOutputJar().get().getAsFile(), getUnpickDefinitions().get().getAsFile());
		fileArg(getConstantJar().getSingleFile());

		// Classpath
		for (Path minecraftJar : getExtension().getMinecraftJars(MappingsNamespace.NAMED)) {
			fileArg(minecraftJar.toFile());
		}

		for (File file : getUnpickClasspath()) {
			fileArg(file);
		}

		writeUnpickLogConfig();
		systemProperty("java.util.logging.config.file", getDirectories().getUnpickLoggingConfigFile().getAbsolutePath());

		super.exec();
	}

	private void writeUnpickLogConfig() {
		try (InputStream is = UnpickJarTask.class.getClassLoader().getResourceAsStream("unpick-logging.properties")) {
			Files.deleteIfExists(getDirectories().getUnpickLoggingConfigFile().toPath());
			Files.copy(is, getDirectories().getUnpickLoggingConfigFile().toPath());
		} catch (IOException e) {
			throw new RuntimeException("Failed to copy unpick logging config", e);
		}
	}

	private void fileArg(File... files) {
		for (File file : files) {
			args(file.getAbsolutePath());
		}
	}

	@Internal
	protected SteelGradleExtension getExtension() {
		return SteelGradleExtension.get(getProject());
	}

	private SteelFiles getDirectories() {
		return getExtension().getFiles();
	}
}
