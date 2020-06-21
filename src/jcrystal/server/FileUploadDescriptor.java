package jcrystal.server;

import jcrystal.db.storage.StorageUtils;

public class FileUploadDescriptor {

	public final String bucketName;
	public final String path;
	public FileUploadDescriptor(String bucketName, String path) {
		this.bucketName = bucketName;
		this.path = path;
	}
	public FileUploadDescriptor(String path) {
		this.bucketName = StorageUtils.getDEFAULT_BUCKET();
		this.path = path;
	}
	
}