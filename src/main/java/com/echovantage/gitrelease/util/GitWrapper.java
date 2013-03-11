package com.echovantage.gitrelease.util;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class GitWrapper {
	
	private final Git git;
	
	public GitWrapper(File basedir, Log log) throws IOException {
		git = Git.open(basedir);
	}

	public boolean hasLocalChanges() {
		try {
			Status status = git.status().call();
			return status.getAdded().isEmpty() &&
					status.getChanged().isEmpty() &&
					status.getConflicting().isEmpty() &&
					status.getMissing().isEmpty() &&
					status.getModified().isEmpty() &&
					status.getRemoved().isEmpty() &&
					status.getUntracked().isEmpty() &&
					status.getUntrackedFolders().isEmpty();
		} catch (NoWorkTreeException | GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	public void createBranch(String tempBranchName) throws Exception {
		git.checkout().setCreateBranch(true).setName(tempBranchName).call();
	}

	public String getCommitHash() {
		try {
			return git.getRepository().getRef(HEAD).getObjectId().getName();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void commit(String message) throws Exception {
		git.commit().setMessage(message).call();
	}

	public void push(String remoteRepoUrl, String ref) throws Exception {
		SshSessionFactory.setInstance(new JschConfigSessionFactory() {
			@Override
			protected void configure(Host hc, Session session) {
				session.setConfig("User", hc.getUser());
			}
		});
		git.push().setRemote(remoteRepoUrl).setRefSpecs(new RefSpec(ref)).call();
	}

	public void tag(String tag, String message) throws Exception {
		git.tag().setName(tag).setMessage(message).call();
	}

	public void checkout(String commit) throws Exception {
		git.checkout().setName(commit).call();
	}

	public String getCurrentBranch() throws IOException {
		return git.getRepository().getRef(HEAD).getLeaf().getName();
	}

	public void merge(String commit) throws Exception {
		ObjectId objectId = git.getRepository().resolve(commit);
		git.merge().include(objectId).call();
	}

}
