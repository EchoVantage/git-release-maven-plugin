package com.echovantage.test;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Test;

import com.echovantage.gitrelease.ScmUrls;

public class WhenParsingScmUrls {
	@Test
	public void getUrlWithoutUsernameOrPassword() {
		assertDeveloperConnection("scm:git:user@host//path/to/repo").parsesTo("user@host//path/to/repo");
	}

//	@Test
//	public void getUrlWithUsernameOnly() {
//		assertDeveloperConnection("scm:hg:ssh://host//path/to/repo").withServer("host", "mdeck", null).parsesTo(
//				"ssh://mdeck@host//path/to/repo");
//	}

	public static ParseAssertion assertDeveloperConnection(String url) {
		ParseAssertion assertion = new ParseAssertion();
		assertion.developerConnection = url;
		return assertion;
	}

	private static class ParseAssertion {
		private String developerConnection;
		private Settings settings = new Settings();

		public ParseAssertion withServer(String id, String username, String password) {
			Server server = new Server();
			server.setId(id);
			server.setUsername(username);
			server.setPassword(password);
			settings.addServer(server);
			return this;
		}

		public void parsesTo(String expectedUrl) {
			Scm scm = new Scm();
			scm.setDeveloperConnection(developerConnection);
			MavenProject project = new MavenProject();
			project.setScm(scm);
			try {
				String actual = ScmUrls.buildRemoteUrl(project, settings);
				assertEquals(expectedUrl, actual);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
