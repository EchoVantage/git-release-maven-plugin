package com.echovantage.gitrelease.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.google.common.base.Joiner;

public class MojoCli {
	public static final String MVN_EXE = isWindows() ? "mvn.bat" : "mvn";
	
	private final Log log;

	public MojoCli(Log log) {
		this.log = log;
	}
	
	private static boolean isWindows() {
		String osName = System.getProperty("os.name");
		if(osName == null) {
			return false;
		}
		return osName.startsWith("Windows");
	}

	protected ProcessBuilder builderFor(String... commandParts) {
		return new ProcessBuilder(commandParts);
	}
	
	public boolean runCommand(String... commandParts) throws MojoExecutionException {
		ProcessBuilder procBuilder = builderFor(commandParts);
		procBuilder.redirectErrorStream(true);
		try {
		Process process = procBuilder.start();
		copyToInfo(process.getInputStream());
		return process.waitFor() == 0;
		} catch (IOException e) {
			throw new MojoExecutionException("Exception while executing commandline process " + joinCommand(commandParts), e);
		} catch (InterruptedException e) {
			throw new MojoExecutionException("Exception while executing commandline process " + joinCommand(commandParts), e);
		}
	}
	
	public boolean runQuietlyOrDie(String... commandParts) throws InterruptedException, IOException, MojoFailureException {
		ProcessBuilder procBuilder = builderFor(commandParts);
		procBuilder.redirectErrorStream(true);
		Process process = procBuilder.start();
		if(process.waitFor() == 0) {
			return true;
		} else {
			copyToError(process.getInputStream());
			throw new MojoFailureException(joinCommand(commandParts) + " did not complete successfully.");
		}
	}

	private String joinCommand(String... commandParts) {
		return Joiner.on(' ').join(commandParts);
	}

	private void copyToError(InputStream errorStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
		String line;
		while((line = reader.readLine()) != null) {
			log.error(line);
		}
	}

	private void copyToInfo(InputStream infoStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(infoStream));
		String line;
		while((line = reader.readLine()) != null) {
			log.info(line);
		}
	}

	public MojoCli fromDir(final File directory) {
		return new MojoCli(log) {
			@Override
			protected ProcessBuilder builderFor(String... commandParts) {
				ProcessBuilder procBuilder = new ProcessBuilder(commandParts);
				procBuilder.directory(directory);
				return procBuilder;
			}
		};
	}
}
