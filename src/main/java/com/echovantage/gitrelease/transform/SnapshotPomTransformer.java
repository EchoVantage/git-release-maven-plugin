package com.echovantage.gitrelease.transform;

import com.echovantage.gitrelease.ReleaseDescriptor;


public class SnapshotPomTransformer extends AbstractPomVersionTransformer {

	@Override
	protected String getVersion(ReleaseDescriptor descriptor) {
		return descriptor.getNewSnapshotVersion();
	}

}
