package com.echovantage.gitrelease;

import static com.echovantage.gitrelease.util.MojoCli.MVN_EXE;
import static java.lang.Integer.toHexString;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Random;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import com.echovantage.gitrelease.transform.ReleasePomTransformer;
import com.echovantage.gitrelease.transform.SnapshotPomTransformer;
import com.echovantage.gitrelease.util.GitWrapper;
import com.echovantage.gitrelease.util.MojoCli;

/**
 * @goal release
 * @aggregator true
 */
public class ReleaseMojo extends AbstractMojo {
	/**
	 * @parameter expression="${reactorProjects}"
	 * @required
	 * @readonly
	 */
	protected List<MavenProject> reactorProjects;

	/**
	 * @parameter expression="${settings}"
	 * @required
	 * @readonly
	 */
	protected Settings settings;

	/** @parameter default-value="${project}" */
	private MavenProject currentProject;


	private String branchName = "mvn_release-" + toHexString(new Random().nextInt());
	private String remoteRepoUrl;
	private ReleaseManager manager;

	private MojoCli mojoCli = new MojoCli(getLog());
	private GitWrapper git;

	private String releaseTag;

	private String origBranch;
	private boolean detachedHeadMode;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		doScmChecks();
		buildReleaseDescriptors();
		doSnapshotChecks();
		recordOrigBranch();
		doBranch();
		updateVersionsToRelease();
		verifyBuild();
		doCommit("git-release plugin: Updating poms to release version.");
		createTag();
		updateVersionsToSnapshot();
		doCommit("git-release plugin: Updating poms to new snapshot version.");
		mergeWithOrigBranch();
		updateToRelease();
		pushTagToRemote();
		doDeploy();
	}

	private void recordOrigBranch() throws MojoExecutionException {
		try {
			origBranch = git.getCurrentBranch();
			if("HEAD".equals(origBranch)) {
				origBranch = git.getCommitHash();
				detachedHeadMode = true;
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error determining current branch", e);
		}
	}

	private void createTag() throws MojoExecutionException {
		String releaseVersion = new ReleaseDescriptor(currentProject).getReleaseVersion();
		releaseTag = "release-" + releaseVersion;
		try {
			git.tag(releaseTag, "Release tag created by Maven git-release plugin.");
		} catch (Exception e) {
			throw new MojoExecutionException("Error creating tag.", e);
		}
	}

	private void doSnapshotChecks() throws MojoFailureException {
		getLog().info("Checking for snapshot dependencies");
		ReleaseIssues issues = manager.findIssues();
		
		if(issues.hasIssues()) {
			getLog().error("The following issues were detected. You must resolve these problems before releasing this project.");
			issues.logIssues(getLog());
			throw new MojoFailureException("Unresolved issues present in project(s).");
		}
	}

	private void buildReleaseDescriptors() {
		manager = new ReleaseManager(reactorProjects);
	}

	private void doScmChecks() throws MojoFailureException, MojoExecutionException {
		getLog().info("Checking SCM status.");
		// must have scm info in pom
		// and scm must be hg
		Scm scm = currentProject.getScm();
		if (scm == null) {
			throw new MojoFailureException("You must define an SCM section of your POM in order to release.");
		}
		String developerConnection = scm.getDeveloperConnection();
		if (developerConnection == null) {
			throw new MojoFailureException(
					"You must define a developerConnection in the SCM section of your POM to release.");
		}

		if (!developerConnection.startsWith("scm:git:")) {
			throw new MojoFailureException(
					"The SCM connection URL you specify must begin with 'scm:git:'. This plugin is not compatible with any other SCM systems besides Git.");
		}

		try {
			remoteRepoUrl = ScmUrls.buildRemoteUrl(currentProject, settings);
		} catch (MalformedURLException e) {
			throw new MojoFailureException("The SCM URL defined in the POM is malformed.", e);
		}

		try {
			git = new GitWrapper(currentProject.getBasedir(), getLog());
		} catch (IOException e) {
			throw new MojoFailureException("Error reading Git repository in project base dir.", e);
		}
		if (git.hasLocalChanges()) {
			throw new MojoFailureException(
					"There are local changes in this workspace. Commit or revert and retry the release.");
		}
	}

	private void doBranch() throws MojoExecutionException, MojoFailureException {
		getLog().info(String.format("Creating and checking out temporary branch (%s)...", branchName));
		try {
			git.createBranch(branchName);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void updateVersionsToRelease() throws MojoExecutionException {
		manager.transformPoms(new ReleasePomTransformer());
	}

	private void verifyBuild() throws MojoExecutionException, MojoFailureException {
		getLog().info("Verifying build...");
		if (!mojoCli.fromDir(currentProject.getBasedir()).runCommand(MVN_EXE, "verify")) {
			throw new MojoFailureException("Build failed");
		}
	}

	private void updateVersionsToSnapshot() throws MojoExecutionException {
		manager.transformPoms(new SnapshotPomTransformer());
	}

	private void doCommit(String message) throws MojoFailureException, MojoExecutionException {
		try {
			git.commit(message);
		} catch (Exception e) {
			throw new MojoFailureException("Failed to commit", e);
		}
	}

	private void pushTagToRemote() throws MojoFailureException, MojoExecutionException {
		try {
			git.push(remoteRepoUrl, releaseTag);
		} catch (Exception e) {
			throw new MojoFailureException("Failed to push to remote: " + remoteRepoUrl, e);
		}
	}

	private void updateToRelease() throws MojoExecutionException {
		try {
			git.checkout(releaseTag);
		} catch (Exception e) {
			throw new MojoExecutionException("Error checking out release tag " + releaseTag, e);
		}
	}

	private void doDeploy() throws MojoExecutionException, MojoFailureException {
		getLog().info("Deploying release...");
		if (!mojoCli.fromDir(currentProject.getBasedir()).runCommand(MVN_EXE, "-D", "performRelease=true", "deploy")) {
			throw new MojoFailureException("Build failed");
		}
	}

	private void mergeWithOrigBranch() throws MojoExecutionException {
		try {
			git.checkout(origBranch);
		} catch (Exception e) {
			throw new MojoExecutionException("Error checking out branch " + origBranch, e);
		}
		
		if(detachedHeadMode) {
			// If HEAD was not pointing to a branch when we started we will simply leave the
			// release branch in place so the user can merge as appropriate.
			
			StringWriter message = new StringWriter();
			PrintWriter w = new PrintWriter(message);
			w.append("*************************************************\n")
			 .append("**************Manual Merge Required**************\n")
			 .append("*************************************************\n")
			 .append("*\n")
			 .printf("* Branch %s has been created for this release.\n", branchName)
			 .append("* It will not be merged or deleted and the\n")
			 .append("* working copy will be returned to the commit\n")
			 .append("* you began the release with.\n")
			 .append("*\n")
			 .append("*************************************************\n");
			
			getLog().info(message.toString());
			return;
		}
		
		try {
			git.merge(branchName);
		} catch (Exception e) {
			throw new MojoExecutionException("Error merging release branch back to originial branch", e);
		}
	}
}
