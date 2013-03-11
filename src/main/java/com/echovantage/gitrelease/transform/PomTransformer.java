package com.echovantage.gitrelease.transform;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import com.echovantage.gitrelease.ReleaseDescriptor;

public interface PomTransformer {
	void transformPoms(Iterable<ReleaseDescriptor> releaseDescriptors) throws IOException, TransformerException;
}
