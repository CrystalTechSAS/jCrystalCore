package jcrystal.server.databases;

import java.util.TreeSet;
import java.util.stream.Collectors;

import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;
import jcrystal.preprocess.responses.ClassOperationType;
import jcrystal.server.Entity;
import jcrystal.server.GeneradorAuthUtils;
import jcrystal.server.databases.google.bigtable.GeneradorGoogleBigtable;
import jcrystal.server.databases.google.bigtable.GoogleBigtableGenerator;
import jcrystal.server.databases.google.datastore.GoogleDatastoreGenerator;
import jcrystal.server.databases.google.datastore.MainEntityGenerator;
import jcrystal.server.databases.google.firebase.firestore.GoogleFirestoreGenerator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class ModelGenerator {
	private ClientContext context;
	
	private jcrystal.server.databases.google.firebase.firestore.MainEntityGenerator googleFirestore;
	private GeneradorGoogleBigtable googleBigtable;
	
	private GeneradorAuthUtils generadorAuthUtils;
	private GeneradorEntityUtils generadorEntityUtils;
	
	public ModelGenerator(ClientContext context) {
		this.context = context;
		generadorAuthUtils = new GeneradorAuthUtils(context);
		generadorEntityUtils = new GeneradorEntityUtils(context);
	}
	public void generarEntidades(BackendWrapper back)throws Exception{
		new ContextGenerator(context).generar();
		if(back.id == null)
			generadorEntityUtils.generateInterface();
		generadorEntityUtils.generateComparisonOperator(back);
		for(EntityClass entidad : back.entitiesList) {
			if(back.id == null) {
			//Elementos adicionales
				generadorAuthUtils.generate(entidad);
				generadorEntityUtils.generateJsonObject(entidad);
				
				if(!entidad.entidad.internal() && !entidad.clase.interfaces.stream().anyMatch(f->f.name().equals(Entity.class.getSimpleName()+"."+entidad.mainDBType.getDBName()))) {
					
					context.output.add(ClassOperationType.ADD_IMPORT, entidad.clase, Entity.class.getName());
					context.output.add(ClassOperationType.ADD_IMPLEMENTS, entidad.clase, Entity.class.getSimpleName()+"."+entidad.mainDBType.getDBName());
				}
			}
		}
		back.entitiesList.stream().collect(Collectors.groupingBy(f->f.mainDBType.type)).forEach((dbType, entities)->{
			switch (dbType) {
				case GOOGLE_DATASTORE:
					new GoogleDatastoreGenerator(back, context, entities).generate();
					break;
				case GOOGLE_FIRESTORE:
					new GoogleFirestoreGenerator(back, context, entities).generate();
					break;
				case GOOGLE_BIG_QUERY:
					new GoogleBigtableGenerator(back, context, entities).generate();
					break;
				default:
					throw new NullPointerException("Unsupported main DB type (" + entities.get(0).name() + "): " + dbType.name());
			}
		});
		
		context.data.databases.forEach((k,db)->{
			new JavaCode(){{
				$("package jcrystal.context;");
				$("public class Utils" + StringUtils.capitalize(db.getDBName()), ()->{
					if(db.type == DBType.GOOGLE_DATASTORE)
						$("public static void clearDB()",()->{
							$if("com.google.appengine.api.utils.SystemProperty.environment.value() == com.google.appengine.api.utils.SystemProperty.Environment.Value.Development",()->{
								for(EntityClass entidad : back.entitiesList) {
									if(entidad.mainDBType.type == db.type && !entidad.entidad.internal())
										$(entidad.batchClassName()+".delete("+entidad.queryClassName()+".getAllKeys());");
								}
							});
						});
					if(db.type == DBType.GOOGLE_DATASTORE)
						$("public static void migrateDBToNamespace(String originNamespace, String targetNamespace)",()->{
							$if("java.util.Objects.equals(originNamespace, targetNamespace)",()->{
								$("throw new jcrystal.errors.ErrorException(\"Can't migrate from \" + originNamespace + \" to \" + targetNamespace);");
							});
							$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
							for(EntityClass entidad : back.entitiesList) {
								if(entidad.mainDBType.type == db.type && !entidad.entidad.internal()) {
									$("com.google.appengine.api.NamespaceManager.set(originNamespace);");
									$("java.util.List<"+entidad.clase.name()+"> " + StringUtils.lowercalize(entidad.clase.getSimpleName()) + " = "+entidad.queryClassName()+".getAll();");
									$("com.google.appengine.api.NamespaceManager.set(targetNamespace);");
									$("$ctx.DefaultDB().service.put("+StringUtils.lowercalize(entidad.clase.getSimpleName()) + ".stream().map(data ->",()->{
										$("com.google.appengine.api.datastore.Entity entity = new com.google.appengine.api.datastore.Entity(" + entidad.clase.name() + ".Key.cloneKey(data.getKey()));");
										$("entity.setPropertiesFrom(data.getRawEntity());");
										$("return entity;");
									},").collect(java.util.stream.Collectors.toList()));");
									$("System.out.println(\"Stored \" + "+StringUtils.lowercalize(entidad.clase.getSimpleName())+".size() + \" entities for " + entidad.clase.name() + "\");");
								}
							}
						});
				});
				context.output.exportFile(this, "jcrystal/context/Utils" + StringUtils.capitalize(db.getDBName())+".java");
			}};
		});
	}
	public jcrystal.server.databases.google.firebase.firestore.MainEntityGenerator getGoogleFirestore() {
		if(googleFirestore == null)
			googleFirestore = new jcrystal.server.databases.google.firebase.firestore.MainEntityGenerator(context); 
		return googleFirestore;
	}
	public GeneradorGoogleBigtable getGoogleBigtable() {
		if(googleBigtable == null)
			googleBigtable = new GeneradorGoogleBigtable(context); 
		return googleBigtable;
	}
}
