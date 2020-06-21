package jcrystal.server;

import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;

import jcrystal.main.data.ClientContext;
import jcrystal.utils.langAndPlats.JavaCode;

public class GenerarUtilidades {

	private ClientContext context;
	
	public GenerarUtilidades(ClientContext context) {
		this.context = context;
	}
	
	public void generar() throws Exception {
		generateAnnotations();
		new JavaCode(){{
			$("package jcrystal.server;");
			$("import java.io.IOException;");
			$("public class FileUploadDescriptor", ()->{
				if(context.input.contains(HttpServlet.class)) {
					$("private javax.servlet.http.Part $fileContentPart;");
					$("public FileUploadDescriptor(javax.servlet.http.Part $fileContentPart)",()->{
						$("this.$fileContentPart = $fileContentPart;");
					});
					$("public String getUserFileName()",()->{
						$("return $fileContentPart.getSubmittedFileName();");
					});
					$("public String getContentType()",()->{
						$("return $fileContentPart.getContentType();");
					});
					$("public long getSize()",()->{
						$("return $fileContentPart.getSize();");
					});
					$("public void put(java.io.File file) throws IOException",()->{
						$("try(java.io.FileOutputStream fos = new java.io.FileOutputStream(file))",()->{
							$("jcrystal.utils.ServletUtils.copy(8*1024, $fileContentPart.getInputStream(), fos);");
						});
					});						
					if(context.input.contains(com.google.appengine.tools.cloudstorage.GcsFileOptions.class)) {
						$("public void put(String path) throws IOException",()->{
							$("this.put(jcrystal.db.storage.StorageUtils.getDEFAULT_BUCKET(), path);");
						});
						$("public void put(String bucketName, String path) throws IOException",()->{
							$("while(path.startsWith(\"/\"))",()->{
								$("path = path.substring(1);");
							});
							$("com.google.appengine.tools.cloudstorage.GcsService gcsService = com.google.appengine.tools.cloudstorage.GcsServiceFactory.createGcsService();");
							$("com.google.appengine.tools.cloudstorage.GcsFileOptions.Builder instance = new com.google.appengine.tools.cloudstorage.GcsFileOptions.Builder();");
							$if("$fileContentPart.getContentType() != null","instance = instance.mimeType($fileContentPart.getContentType());");
							$if("$fileContentPart.getSubmittedFileName() != null","instance = instance.addUserMetadata(\"filename\", $fileContentPart.getSubmittedFileName());");
							$("com.google.appengine.tools.cloudstorage.GcsOutputChannel outputChannel = gcsService.createOrReplace(new com.google.appengine.tools.cloudstorage.GcsFilename(bucketName, path), instance.build());");
							$("jcrystal.utils.ServletUtils.copy(8*1024, $fileContentPart.getInputStream(), java.nio.channels.Channels.newOutputStream(outputChannel));");
						});
						$("public static void delete(String path) throws IOException",()->{
							$("delete(jcrystal.db.storage.StorageUtils.getDEFAULT_BUCKET(), path);");
						});
						$("public static void delete(String bucketName, String path) throws IOException",()->{
							$("while(path.startsWith(\"/\"))",()->{
								$("path = path.substring(1);");
							});
							$("com.google.appengine.tools.cloudstorage.GcsService gcsService = com.google.appengine.tools.cloudstorage.GcsServiceFactory.createGcsService();");
							$("gcsService.delete(new com.google.appengine.tools.cloudstorage.GcsFilename(bucketName, path));");
						});
					}
				}
			});
			context.output.exportFile(this, "jcrystal/server/FileUploadDescriptor.java");
		}};
		if(context.input.contains(com.google.appengine.tools.cloudstorage.GcsFileOptions.class))
			context.output.addResource(GenerarUtilidades.class.getResourceAsStream("files/StorageUtils"), "jcrystal/api/utils/StorageUtils.java");
		
	}
	private void generateAnnotations() {
		if(!context.input.SERVER.BACKENDS.isEmpty()) {
			new JavaCode(){{
				$("package jcrystal.annotations.server;");
				$("import java.lang.annotation.Retention;");
				$("import java.lang.annotation.RetentionPolicy;");
				$("@Retention(RetentionPolicy.RUNTIME)");
				$("public @interface Exposed", ()->{
					$("public Back[] value() default {};");
					$("public enum Back",()->{
						$(context.input.SERVER.BACKENDS.values().stream().map(f->f.id).collect(Collectors.joining(", ")));
					});
				});
				context.output.exportFile(this, "jcrystal/annotations/server/Exposed.java");
			}};
		}
	}
}
