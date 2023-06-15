/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2022 HypherionSA and Contributors
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

package net.flintloader.steel.api;

import java.util.Set;

import javax.inject.Inject;

import com.google.common.base.Preconditions;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Named} object for configuring "proxy" configurations that remap artifacts.
 */
public abstract class RemapConfigurationSettings implements Named {
	private final String name;

	@Inject
	public RemapConfigurationSettings(String name) {
		this.name = name;

		getSourceSet().finalizeValueOnRead();
		getTargetConfigurationName().finalizeValueOnRead();
		getClientSourceConfigurationName().finalizeValueOnRead();
		getOnCompileClasspath().finalizeValueOnRead();
		getOnRuntimeClasspath().finalizeValueOnRead();
		getPublishingMode().convention(PublishingMode.NONE).finalizeValueOnRead();
	}

	@Override
	public @NotNull String getName() {
		return name;
	}

	/**
	 * @return The target source set
	 */
	public abstract Property<SourceSet> getSourceSet();

	/**
	 * @return The target configuration name
	 */
	public abstract Property<String> getTargetConfigurationName();

	/**
	 * Optional, only used when split sourcesets are enabled.
	 * When not present client only entries should go onto the target configuration.
	 *
	 * @return The client source configuration name
	 */
	public abstract Property<String> getClientSourceConfigurationName();

	/**
	 * @return True if this configuration's artifacts should be exposed for compile operations.
	 */
	public abstract Property<Boolean> getOnCompileClasspath();

	/**
	 * @return True if this configuration's artifacts should be exposed to runtime operations.
	 */
	public abstract Property<Boolean> getOnRuntimeClasspath();

	/**
	 * @return the {@link PublishingMode} for the configuration.
	 */
	public abstract Property<PublishingMode> getPublishingMode();

	public enum PublishingMode {
		NONE,
		COMPILE_ONLY(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME),
		RUNTIME_ONLY(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME),
		COMPILE_AND_RUNTIME(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME);

		private final Set<String> outgoingConfigurations;

		PublishingMode(String... outgoingConfigurations) {
			this.outgoingConfigurations = Set.of(outgoingConfigurations);
		}

		public Set<String> outgoingConfigurations() {
			return outgoingConfigurations;
		}
	}

	@Inject
	protected abstract Project getProject();

	@ApiStatus.Internal
	@Internal
	public final String getRemappedConfigurationName() {
		return getName() + "Mapped";
	}

	@ApiStatus.Internal
	@Internal
	public final Provider<String> getClientRemappedConfigurationName() {
		return getClientSourceConfigurationName().map(s -> s + "Mapped");
	}

	@ApiStatus.Internal
	@Internal
	public final NamedDomainObjectProvider<Configuration> getSourceConfiguration() {
		return getConfigurationByName(getName());
	}

	@ApiStatus.Internal
	@Internal
	public final NamedDomainObjectProvider<Configuration> getRemappedConfiguration() {
		return getConfigurationByName(getRemappedConfigurationName());
	}

	@ApiStatus.Internal
	@Internal
	public final NamedDomainObjectProvider<Configuration> getTargetConfiguration() {
		return getConfigurationByName(getTargetConfigurationName().get());
	}

	@ApiStatus.Internal
	@Internal
	public final Provider<Configuration> getClientTargetConfiguration() {
		return getProject().provider(() -> {
			boolean split = false;
			Preconditions.checkArgument(split, "Cannot get client target configuration when project is not split");
			return getConfigurationByName(getClientSourceConfigurationName().get()).get();
		});
	}

	@ApiStatus.Internal
	@Internal
	public final Provider<Configuration> getClientRemappedConfiguration() {
		return getProject().provider(() -> {
			boolean split = false;
			Preconditions.checkArgument(split, "Cannot get client remapped configuration when project is not split");
			return getConfigurationByName(getClientRemappedConfigurationName().get()).get();
		});
	}

	@Internal
	private NamedDomainObjectProvider<Configuration> getConfigurationByName(String name) {
		return getProject().getConfigurations().named(name);
	}
}
