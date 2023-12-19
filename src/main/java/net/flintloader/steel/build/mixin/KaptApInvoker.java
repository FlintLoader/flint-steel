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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import kotlin.Unit;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.kotlin.gradle.plugin.KaptExtension;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.extension.MixinExtension;

public class KaptApInvoker extends AnnotationProcessorInvoker<JavaCompile> {
	private final KaptExtension kaptExtension = project.getExtensions().getByType(KaptExtension.class);
	// Refmap will be written to here with mixin, then moved after JavaCompile to the correct place
	private final File dummyRefmapDirectory;

	public KaptApInvoker(Project project) {
		super(
				project,
				getApConfigurations(project, KaptApInvoker::getKaptConfigurationName),
				getInvokerTasks(project));

		try {
			dummyRefmapDirectory = Files.createTempDirectory("temp_refmap").toFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		dummyRefmapDirectory.deleteOnExit();

		// Needed for mixin AP to run
		kaptExtension.setIncludeCompileClasspath(false);
	}

	private static Map<SourceSet, JavaCompile> getInvokerTasks(Project project) {
		MixinExtension mixin = SteelGradleExtension.get(project).getMixin();
		return mixin.getInvokerTasksStream(JAVA)
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> Objects.requireNonNull((JavaCompile) entry.getValue())));
	}

	@Override
	public void configureMixin() {
		super.configureMixin();

		for (Map.Entry<SourceSet, JavaCompile> entry : invokerTasks.entrySet()) {
			// Kapt only allows specifying javac args to all annotation processors at once. So we need to specify some dummy
			// target location for the refmap and then move it to the correct place for each sourceset
			JavaCompile task = entry.getValue();
			SourceSet sourceSet = entry.getKey();
			task.doLast(t -> {
				try {
					String refmapName = Objects.requireNonNull(MixinExtension.getMixinInformationContainer(sourceSet)).refmapNameProvider().get();
					Path src = Paths.get(getRefmapDestination(task, refmapName));
					Path dest = Paths.get(task.getDestinationDirectory().get().getAsFile().toString(), refmapName);

					// Possible that no mixin annotations exist
					if (Files.exists(src)) {
						project.getLogger().info("Copying refmap from " + src + " to " + dest);
						Files.move(src, dest);
					}
				} catch (IOException e) {
					project.getLogger().warn("Could not move refmap generated by kapt for task " + task, e);
				}
			});
		}
	}

	// Pulled out from the internal class: https://github.com/JetBrains/kotlin/blob/33a0ec9b4f40f3d6f1f96b2db504ade4c2fafe03/libraries/tools/kotlin-gradle-plugin/src/main/kotlin/org/jetbrains/kotlin/gradle/internal/kapt/Kapt3KotlinGradleSubplugin.kt#L92
	private static String getKaptConfigurationName(SourceSet sourceSet) {
		String sourceSetName = sourceSet.getName();

		if (!sourceSetName.equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
			return "kapt" + (sourceSetName.substring(0, 1).toUpperCase() + sourceSetName.substring(1));
		}

		return "kapt";
	}

	@Override
	protected void passArgument(JavaCompile compileTask, String key, String value) {
		// Note: this MUST be run early on, before kapt uses this data, and there is only a point to setting the value once since
		// kapt shares the options with all java compilers
		kaptExtension.arguments(args -> {
			args.arg(key, value);
			return Unit.INSTANCE;
		});
	}

	@Override
	protected File getRefmapDestinationDir(JavaCompile task) {
		return dummyRefmapDirectory;
	}
}
