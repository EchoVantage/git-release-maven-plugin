package com.echovantage.gitrelease.transform;

import com.echovantage.gitrelease.ReleaseDescriptor;

public class ReleasePomTransformer extends AbstractPomVersionTransformer {
	@Override
	protected String getVersion(ReleaseDescriptor descriptor) {
		return descriptor.getReleaseVersion();
	}

}
