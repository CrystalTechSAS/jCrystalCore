package jcrystal.server.databases.google.firebase.realtimedb;


import java.io.FileInputStream;

import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.server.databases.AbsContextGenerator;

public class ContextGenerator extends AbsContextGenerator {

	public ContextGenerator(ClientContext context) {
		super(DBType.GOOGLE_REALTIMEDB, context);
	}

	@Override
	protected void generateContent() {
		$("private static com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleCredential credentials;");
		$("public static void initialize(String databaseUrl)",()->{
			$("try",()->{
				$if("com.google.appengine.api.utils.SystemProperty.environment.value() == com.google.appengine.api.utils.SystemProperty.Environment.Value.Development",()->{
					if(context.input.SERVER.INTEGRATION.FIREBASE.devAccountService != null) {
						$("try(java.io.FileInputStream accountServicePrivateFile = new java.io.FileInputStream(\"" + context.input.SERVER.INTEGRATION.FIREBASE.devAccountService + "\"))",()->{
							$("com.google.firebase.FirebaseOptions options = new com.google.firebase.FirebaseOptions.Builder().setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(accountServicePrivateFile)).setDatabaseUrl(databaseUrl).build();");
							$("com.google.firebase.FirebaseApp.initializeApp(options);");
						});
					}
				});
				$else(()->{
					if(context.input.SERVER.INTEGRATION.FIREBASE.prodAccountService != null) {
						$("try(java.io.FileInputStream accountServicePrivateFile = new java.io.FileInputStream(\"" + context.input.SERVER.INTEGRATION.FIREBASE.prodAccountService + "\"))",()->{
							$("com.google.firebase.FirebaseOptions options = new com.google.firebase.FirebaseOptions.Builder().setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(accountServicePrivateFile)).setDatabaseUrl(databaseUrl).build();");
							$("com.google.firebase.FirebaseApp.initializeApp(options);");
						});
					}else {
						$("com.google.firebase.FirebaseOptions options = new com.google.firebase.FirebaseOptions.Builder().setCredentials(com.google.auth.oauth2.GoogleCredentials.getApplicationDefault()).setDatabaseUrl(databaseUrl).build();");
						$("com.google.firebase.FirebaseApp.initializeApp(options);");
					}
				});
			});
			$catch("java.io.IOException ex", ()->{
				$("ex.printStackTrace();");
			});	
		});
		$("public "+className()+"()",()->{ 
		});
		$("public static com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleCredential credentials()", () -> {
			$if("credentials == null",()->{
				$("try",()->{
					$("credentials = com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleCredential.getApplicationDefault().createScoped(java.util.Arrays.asList(\"https://www.googleapis.com/auth/userinfo.email\",\"https://www.googleapis.com/auth/firebase.database\"));");
				});
				$SingleCatch("Exception ex",  "ex.printStackTrace();");
			});
			$("return credentials;");
		});
	}

}
