package com.echovantage.gitrelease.transform;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.echovantage.gitrelease.ReleaseDescriptor;

public abstract class AbstractPomVersionTransformer implements PomTransformer {

	private static final String PREFIX = buildPrefix();

	protected abstract String getVersion(ReleaseDescriptor descriptor);

	private static final String PARENT_TRANSFORM = "<xsl:template match=\"/mvn:project/mvn:parent[mvn:artifactId='%s' and mvn:groupId='%s']/mvn:version/text()\">%s</xsl:template>";
	private static final String PLUGIN_TRANSFORM = "<xsl:template match=\"//mvn:plugin[mvn:artifactId='%s' and mvn:groupId='%s']/mvn:version/text()\">%s</xsl:template>";
	private static final String DEPENDENCY_TRANSFORM = "<xsl:template match=\"//mvn:dependency[mvn:artifactId='%s' and mvn:groupId='%s']/mvn:version/text()\">%s</xsl:template>";
	private static final String PROJECT_VERSION_TRANSFORM = "<xsl:template match=\"/mvn:project/mvn:version/text()\">%s</xsl:template>";
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	private static String buildPrefix() {
		StringBuilder sb = new StringBuilder();
		sb.append("<xsl:stylesheet version=\"1.0\"\n");
		sb.append("   xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n");
		sb.append("   xmlns:mvn=\"http://maven.apache.org/POM/4.0.0\">\n");
		sb.append("   <xsl:output omit-xml-declaration=\"yes\" />");
		sb.append("   <xsl:template match=\"node()|@*\">\n");
		sb.append("      <xsl:copy>\n");
		sb.append("         <xsl:apply-templates select=\"node()|@*\" />\n");
		sb.append("      </xsl:copy>\n");
		sb.append("   </xsl:template>\n");
		return sb.toString();
	}

	private static void transform(File pomFile, String xslt) throws IOException, TransformerException {
		StreamSource xsltSource = new StreamSource(new StringReader(xslt));
		Transformer transformer = transformerFactory.newTransformer(xsltSource);
		Reader pomFileReader = new FileReader(pomFile);
		StreamSource source = new StreamSource(pomFileReader);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(out);
		try {
			transformer.transform(source, result);
		} finally {
			pomFileReader.close();
		}
		OutputStream pomOutStream = new FileOutputStream(pomFile);
		try {
			pomOutStream.write(out.toByteArray());
		} finally {
			pomOutStream.close();
		}
	}

	@Override
	public void transformPoms(Iterable<ReleaseDescriptor> descriptors) throws IOException, TransformerException {
		String xsltPrefix = PREFIX + buildDependencyTransforms(descriptors);
		for(ReleaseDescriptor descriptor : descriptors) {
			StringBuilder xslt = new StringBuilder(xsltPrefix);
			xslt.append(format(PROJECT_VERSION_TRANSFORM, getVersion(descriptor)));
			xslt.append("</xsl:stylesheet>");
			transform(descriptor.getPomFile(), xslt.toString());
		}
	}

	public AbstractPomVersionTransformer() {
		super();
	}

	private String buildDependencyTransforms(Iterable<ReleaseDescriptor> descriptors) {
		StringBuilder sb = new StringBuilder();
		for(ReleaseDescriptor descriptor : descriptors) {
			sb.append(buildDependencyTransform(descriptor)).append("\n");
		}
		return sb.toString();
	}

	private String buildDependencyTransform(ReleaseDescriptor descriptor) {
		if("pom".equals(descriptor.getPackaging())) {
			return parentTransform(descriptor);
		}
		
		if("maven-plugin".equals(descriptor.getPackaging())) {
			return pluginTransform(descriptor);
		}
		
		return dependencyTransform(descriptor);
	}

	private String dependencyTransform(ReleaseDescriptor descriptor) {
		return String.format(DEPENDENCY_TRANSFORM, descriptor.getArtifactId(), descriptor.getGroupId(), getVersion(descriptor));
	}

	private String pluginTransform(ReleaseDescriptor descriptor) {
		return String.format(PLUGIN_TRANSFORM, descriptor.getArtifactId(), descriptor.getGroupId(), getVersion(descriptor));
	}

	private String parentTransform(ReleaseDescriptor descriptor) {
		return String.format(PARENT_TRANSFORM, descriptor.getArtifactId(), descriptor.getGroupId(), getVersion(descriptor));
	}

}