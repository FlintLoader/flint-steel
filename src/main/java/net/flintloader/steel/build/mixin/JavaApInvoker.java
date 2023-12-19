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

package net.flintloader.steel.build.mixin;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.extension.MixinExtension;

public class JavaApInvoker extends AnnotationProcessorInvoker<JavaCompile> {
	public JavaApInvoker(Project project) {
		super(
				project,
				getApConfigurations(project, SourceSet::getAnnotationProcessorConfigurationName),
				getInvokerTasks(project));
	}

	private static Map<SourceSet, JavaCompile> getInvokerTasks(Project project) {
		MixinExtension mixin = SteelGradleExtension.get(project).getMixin();
		return mixin.getInvokerTasksStream(JAVA)
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> Objects.requireNonNull((JavaCompile) entry.getValue())));
	}

	@Override
	protected void passArgument(JavaCompile compileTask, String key, String value) {
		compileTask.getOptions().getCompilerArgs().add("-A" + key + "=" + value);
	}

	@Override
	protected File getRefmapDestinationDir(JavaCompile task) {
		return task.getDestinationDirectory().getAsFile().get();
	}
}
