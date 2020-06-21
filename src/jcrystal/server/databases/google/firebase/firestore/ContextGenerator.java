package jcrystal.server.databases.google.firebase.firestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.server.databases.AbsContextGenerator;

public class ContextGenerator extends AbsContextGenerator {

	public ContextGenerator(ClientContext context) {
		super(DBType.GOOGLE_FIRESTORE, context);
	}

	@Override
	protected void generateContent() {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(jcrystal.server.databases.google.firebase.firestore.MainEntityGenerator.class.getResourceAsStream("GoogleFirestore")))) {
			for(String h; (h=br.readLine()) != null;)
				$(h);
		} catch (IOException e) {
			e.printStackTrace();
		}
		context.data.databases.forEach((k,db)->{
			if(db.type == type) {
				$("public com.google.api.core.ApiFuture<java.util.List<com.google.cloud.firestore.WriteResult>> createBatch(java.util.stream.Stream<jcrystal.server.Entity."+db.getDBName()+"> entities)",()->{
					$("com.google.cloud.firestore.WriteBatch batch = service.batch();");
					$("entities.forEach(entity -> batch.create(service.document(entity.getRawKeyPath()), entity.getRawEntity()));");
					$("return batch.commit();");
				});		
			}
		});
	}

}
