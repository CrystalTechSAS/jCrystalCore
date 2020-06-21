package jcrystal.server.databases.google.datastore;

import java.util.List;
import java.util.ListIterator;

import jcrystal.entity.types.PersistentFile;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.server.databases.AbsDBGenerator;
import jcrystal.server.databases.datastore.FileData;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.utils.langAndPlats.JavaCode;

public class GoogleDatastoreGenerator extends AbsDBGenerator{
	MainEntityGenerator googleDatastore;
	IJType fileDataType;
	public GoogleDatastoreGenerator(BackendWrapper back, ClientContext context, List<EntityClass> entities) {
		super(back, context, entities);
		googleDatastore = new MainEntityGenerator(context);
	}
	void preprocess() {
		fileDataType = context.input.jClassResolver.forName(FileData.class.getName());
		if(fileDataType == null && entities.stream().anyMatch(entity->entity.properties.stream().anyMatch(f->f.type().is(PersistentFile.class)))) {
			fileDataType = new JClass(context.input.jClassResolver, FileData.class).load(FileData.class);
			new JavaCode() {{
				$("package " + FileData.class.getPackage().getName() + ";");
				$("import jcrystal.reflection.annotations.EntityProperty;");
				$("import jcrystal.reflection.annotations.jEntity;");
				$("@jEntity(internal = true)");
				$("public class FileData",()->{
					$("@EntityProperty");
					$("private static String name;");
					$("@EntityProperty");
					$("private static String mimetype;");
					$("@EntityProperty");
					$("private static long length;");
				});
				context.output.exportFile(back, this, FileData.class.getName().replace(".", "/")+".java");
			}};
		}
		for(EntityClass entity : entities) {
			ListIterator<EntityField> iterator = entity.properties.listIterator();
			while(iterator.hasNext()) {
				EntityField field = iterator.next();
				if(field.type().is(PersistentFile.class)) {
					if(fileDataType != null) {
						field.f.type = fileDataType;
						field.setTargetEntity(context.data.entidades.get(fileDataType));
					}else
						iterator.remove();
				}
			}
		}
	}
	@Override
	public void generate() {
		preprocess();
		for(EntityClass entity : entities)
			googleDatastore.generate(back, entity, false);
		for(EntityClass entity : getKeysOnlyEntities())
			googleDatastore.generate(back, entity, true);
		IndexGenerator.generateIndexFiles(context, entities);
		generateFileData();
	}
	public void generateFileData() {
		
	}
}
