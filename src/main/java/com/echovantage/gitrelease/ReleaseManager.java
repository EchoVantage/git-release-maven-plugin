package com.echovantage.gitrelease;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.echovantage.gitrelease.transform.PomTransformer;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class ReleaseManager {
	private final Iterable<MavenProject> reactorProjects;
	private final Iterable<ReleaseDescriptor> descriptors;

	public ReleaseManager(Iterable<MavenProject> reactorProjects) {
		this.reactorProjects = reactorProjects;
		this.descriptors = Iterables.transform(reactorProjects, new Function<MavenProject, ReleaseDescriptor>() {
			@Override
			public ReleaseDescriptor apply(MavenProject input) {
				return new ReleaseDescriptor(input);
			}
		});
	}
	
	public ReleaseIssues findIssues() {
		ReleaseIssues issues = new ReleaseIssues();
		for(MavenProject project : reactorProjects) {
			findIssues(project, issues);
		}
		return issues;
	}

	private void findIssues(MavenProject project, ReleaseIssues issues) {
		for (Dependency dependency : project.getDependencies()) {
			if(!validateDependency(dependency)) {
				issues.addSnapshotDependency(project, dependency);
			}
		}

		DependencyManagement dependencyManagement = project.getDependencyManagement();
		if (dependencyManagement != null) {
			for(Dependency dependency : dependencyManagement.getDependencies()) {
				if(!validateDependency(dependency)) {
					issues.addSnapshotManagedDependency(project, dependency);
				}
			}
		}

		for (Plugin plugin : project.getBuildPlugins()) {
			if (!validatePlugin(plugin)) {
				issues.addSnapshotPlugin(project, plugin);
			}
		}

		PluginManagement pluginManagement = project.getPluginManagement();
		if (pluginManagement != null) {
			for (Plugin plugin : pluginManagement.getPlugins()) {
				if (!validatePlugin(plugin)) {
					issues.addSnapshotManagedPlugin(project, plugin);
				}
			}
		}
	}

	private boolean validateDependency(Dependency dependency) {
		return validateDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
	}

	private boolean validatePlugin(Plugin plugin) {
		return validateDependency(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
	}
	
	private boolean validateDependency(String groupId, String artifactId, String version) {
		if(version == null) {
			return true;
		}
		if(!version.endsWith("-SNAPSHOT")) {
			return true;
		}
		
		return isReleaseProject(groupId, artifactId, version);
	}
	
	private boolean isReleaseProject(String groupId, String artifactId, String version) {
		for(MavenProject project : reactorProjects) {
			if(groupId.equals(project.getGroupId()) && artifactId.equals(project.getArtifactId()) && version.equals(project.getVersion())) {
				return true;
			}
		}
		return false;
	}

	public void transformPoms(PomTransformer transformer) throws MojoExecutionException {
		try {
			transformer.transformPoms(descriptors);
		} catch (IOException e) {
			throw new MojoExecutionException("Error transforming POM", e);
		} catch (TransformerException e) {
			throw new MojoExecutionException("Error transforming POM", e);
		}
	}
}
