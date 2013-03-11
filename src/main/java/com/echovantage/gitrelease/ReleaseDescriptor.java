package com.echovantage.gitrelease;

import java.io.File;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;

public class ReleaseDescriptor {

	private final MavenProject project;

	public ReleaseDescriptor(MavenProject project) {
		this.project = project;
	}

	public String getArtifactId() {
		return project.getArtifactId();
	}

	public String getGroupId() {
		return project.getGroupId();
	}

	public String getCurrentVersion() {
		return project.getVersion();
	}

	public String getReleaseVersion() {
		String currentVersion = project.getVersion();
		int snapshotIndex = currentVersion.indexOf("-SNAPSHOT");
		if (snapshotIndex == -1) {
			return currentVersion;
		}
		return currentVersion.substring(0, snapshotIndex);
	}

	public String getNewSnapshotVersion() {
		if (!isSnapshot()) {
			throw new IllegalStateException(
					"Cannot create new snapshot version for an artifact that is not already in a snapshot state.");
		}
		String releaseVersion = getReleaseVersion();
		int lastDot = releaseVersion.lastIndexOf('.');
		String minorVersion = releaseVersion.substring(lastDot + 1);
		int newMinorVersion = Integer.parseInt(minorVersion) + 1;
		return releaseVersion.substring(0, lastDot) + '.' + newMinorVersion + "-SNAPSHOT";
	}

	private boolean isSnapshot() {
		String currentVersion = project.getVersion();
		int snapshotIndex = currentVersion.indexOf("-SNAPSHOT");
		return snapshotIndex != -1;
	}

	public File getPomFile() {
		return project.getFile();
	}

	public String getPackaging() {
		return project.getPackaging();
	}

	public void accumulateSnapshotIssues(ReleaseIssues issues) {
		for (Dependency dependency : project.getDependencies()) {
			if (dependency.getVersion() != null && dependency.getVersion().endsWith("-SNAPSHOT")) {
				issues.addSnapshotDependency(project, dependency);
			}
		}

		DependencyManagement dependencyManagement = project.getDependencyManagement();
		if (dependencyManagement != null) {
			for (Dependency dependency : dependencyManagement.getDependencies()) {
				if (dependency.getVersion() != null && dependency.getVersion().endsWith("-SNAPSHOT")) {
					issues.addSnapshotManagedDependency(project, dependency);
				}
			}
		}

		for (Plugin plugin : project.getBuildPlugins()) {
			if (plugin.getVersion() != null && plugin.getVersion().endsWith("-SNAPSHOT")) {
				issues.addSnapshotPlugin(project, plugin);
			}
		}

		PluginManagement pluginManagement = project.getPluginManagement();
		if (pluginManagement != null) {
			for (Plugin plugin : pluginManagement.getPlugins()) {
				if (plugin.getVersion() != null && plugin.getVersion().endsWith("-SNAPSHOT")) {
					issues.addSnapshotManagedPlugin(project, plugin);
				}
			}
		}
	}
}