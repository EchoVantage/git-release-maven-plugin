package com.echovantage.gitrelease;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.provider.ScmUrlUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

public class ScmUrls {
	public static String buildRemoteUrl(MavenProject project, Settings settings) throws MalformedURLException {
		String developerConnection = project.getScm().getDeveloperConnection();
		String rawUrl = ScmUrlUtils.getProviderSpecificPart(developerConnection);
		return rawUrl;
//		URL url = new URL(null, rawUrl, new URLStreamHandler() {
//			@Override
//			protected URLConnection openConnection(URL u) throws IOException {
//				throw new RuntimeException("Not Yet Implemented");
//			}
//		});
//		String host = url.getHost();
//		Server server = settings.getServer(host);
//		String username = null;
//		if(server != null) {
//			username = server.getUsername();
//		}
//		
//		StringBuilder newUrl = new StringBuilder();
//		newUrl.append(url.getProtocol()).append("://");
//		if(username != null) {
//			newUrl.append(username).append("@");
//		}
//		newUrl.append(url.getHost());
//		newUrl.append(url.getPath());
//		return newUrl.toString();
	}
}
