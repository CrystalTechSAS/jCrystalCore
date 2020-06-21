package jcrystal.api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;

public class StorageUtils {
	private static String DEFAULT_BUCKET;
	private static String getDEFAULT_BUCKET() {
		if(DEFAULT_BUCKET == null)
			return DEFAULT_BUCKET = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
		return DEFAULT_BUCKET;
	}
	private final GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
		      .initialRetryDelayMillis(10)
		      .retryMaxAttempts(10)
		      .totalRetryPeriodMillis(15000)
		      .build());
	private GcsFileOptions.Builder instance = new GcsFileOptions.Builder();
	public StorageUtils mimeType(String mimeType) {
		instance.mimeType(mimeType);
		return this;
	}
	public void upload(String path, InputStream is) throws IOException {
		
		try(GcsOutputChannel outputChannel = gcsService.createOrReplace(new GcsFilename(getDEFAULT_BUCKET(), path), instance.build())){
			copy(is, Channels.newOutputStream(outputChannel));
		}
		
	}
	public void upload(String path, byte[] array) throws IOException {
		try(GcsOutputChannel outputChannel = gcsService.createOrReplace(new GcsFilename(getDEFAULT_BUCKET(), path), instance.build())){
			outputChannel.write(ByteBuffer.wrap(array));
		}
	}
	private void copy(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buffer = new byte[10*1024];
			int bytesRead = input.read(buffer);
			while (bytesRead != -1) {
				output.write(buffer, 0, bytesRead);
				bytesRead = input.read(buffer);
			}
		    } finally {
			    input.close();
			    output.close();
		    }
	  }
}
