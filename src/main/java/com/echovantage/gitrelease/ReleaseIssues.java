package com.echovantage.gitrelease;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class ReleaseIssues {
	private Map<MavenProject, ProjectIssues> projectIssues = new HashMap<MavenProject, ReleaseIssues.ProjectIssues>();

	public void addSnapshotDependency(MavenProject project, Dependency dependency) {
		issuesFor(project).addSnapshotDependency(dependency);
	}

	public void addSnapshotManagedDependency(MavenProject project, Dependency dependency) {
		issuesFor(project).addSnapshotManagedDependency(dependency);
	}

	public void addSnapshotPlugin(MavenProject project, Plugin plugin) {
		issuesFor(project).addSnapshotPlugin(plugin);
	}

	public void addSnapshotManagedPlugin(MavenProject project, Plugin plugin) {
		issuesFor(project).addSnapshotManagedPlugin(plugin);
	}

	public boolean hasIssues() {
		for(ProjectIssues pi : projectIssues.values()) {
			if(pi.hasIssues()) {
				return true;
			}
		}
		return false;
	}
	
	public void logIssues(Log log) {
		for(Entry<MavenProject, ProjectIssues> entry : projectIssues.entrySet()) {
			MavenProject project = entry.getKey();
			ProjectIssues issues = entry.getValue();
			log.error(String.format("%s:%s:", project.getGroupId(), project.getArtifactId()));
			issues.logIssues(log);
		}
	}
	
	private ProjectIssues issuesFor(MavenProject project) {
		if(projectIssues.containsKey(project)) {
			return projectIssues.get(project);
		}
		ProjectIssues pi = new ProjectIssues();
		projectIssues.put(project, pi);
		return pi;
	}
	
	private static class ProjectIssues {
		private final Collection<Dependency> snapshotDependencies = new ArrayList<Dependency>();
		private final Collection<Dependency> managedSnapshotDependencies = new ArrayList<Dependency>();
		private final Collection<Plugin> snapshotPlugins = new ArrayList<Plugin>();
		private final Collection<Plugin> managedSnapshotPlugins = new ArrayList<Plugin>();

		public void addSnapshotDependency(Dependency dependency) {
			snapshotDependencies.add(dependency);
		}

		public void addSnapshotManagedDependency(Dependency dependency) {
			managedSnapshotDependencies.add(dependency);
		}
		
		public void addSnapshotPlugin(Plugin plugin) {
			snapshotPlugins.add(plugin);
		}

		public void addSnapshotManagedPlugin(Plugin plugin) {
			managedSnapshotPlugins.add(plugin);
		}
		
		public boolean hasIssues() {
			return !(snapshotDependencies.isEmpty() && managedSnapshotDependencies.isEmpty() && snapshotPlugins.isEmpty() && managedSnapshotPlugins.isEmpty());
		}
		
		public void logIssues(Log log) {
			if(!hasIssues()) {
				log.error("\tno issues");
				return;
			}
			if(!snapshotDependencies.isEmpty()) {
				log.error("\tSnapshot Dependencies:");
				for(Dependency dependency : snapshotDependencies) {
					log.error(String.format("\t\t%s:%s:%s", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
				}
			}
			if(!managedSnapshotDependencies.isEmpty()) {
				log.error("\tManaged Snapshot Dependencies:");
				for(Dependency dependency : managedSnapshotDependencies) {
					log.error(String.format("\t\t%s:%s:%s", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
				}
			}
			if(!snapshotPlugins.isEmpty()) {
				log.error("\tSnapshot Plugins:");
				for(Plugin plugin : snapshotPlugins) {
					log.error(String.format("\t\t%s:%s:%s", plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion()));
				}
			}
			if(!managedSnapshotPlugins.isEmpty()) {
				log.error("\tManaged Snapshot Plugins:");
				for(Plugin plugin : managedSnapshotPlugins) {
					log.error(String.format("\t\t%s:%s:%s", plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion()));
				}
			}
		}
	}
	
}
