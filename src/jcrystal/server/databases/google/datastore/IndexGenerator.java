package jcrystal.server.databases.google.datastore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jcrystal.configs.clients.ResourceType;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.IndexableField;
import jcrystal.model.server.db.EntityIndexModel;
import jcrystal.reflection.annotations.IndexType;

public class IndexGenerator {
	public static void generateIndexFiles(ClientContext context, List<EntityClass> entities){
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			pw.println("indexes:");
			pw.println();
			for (EntityClass ce : entities) {
				if(ce.key != null && !ce.key.isSimple())
					for(EntityField field : ce.properties)
						if(field.indexType != IndexType.NONE) {
							pw.println("- kind: " + ce.name());
							pw.println("  ancestor: yes");
							pw.println("  properties:");
								pw.println("  - name: " + field.getDBName());
								pw.println("    direction: desc");
							pw.println();
						}
							
				for (EntityIndexModel ind : ce.indices) {
					pw.println("- kind: " + ce.name());
					pw.println("  ancestor: no");
					pw.println("  properties:");
					for (IndexableField field : ind.camposIndice) {
						pw.println("  - name: " + field.getDBName());
						pw.println("    direction: desc");
					}
					pw.println();
				}
			}
		}
		context.output.send(ResourceType.WEB_INF, sw.toString(), "index.yaml");
		sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<datastore-indexes autoGenerate=\"true\">");
			for (EntityClass ce : entities) {
				if(ce.key != null && !ce.key.isSimple())
					for(EntityField field : ce.properties)
						if(field.indexType != IndexType.NONE) {
							pw.println("	<datastore-index kind=\"" + ce.name() + "\" ancestor=\"true\">");
							pw.println("		<property name=\"" + field.getDBName() + "\" direction=\"desc\"/>");
							pw.println("	</datastore-index>");
						}
							
				for (EntityIndexModel ind : ce.indices) {
					pw.println("	<datastore-index kind=\"" + ce.name() + "\" ancestor=\"false\">");
					for (IndexableField field : ind.camposIndice) {
						pw.println("		<property name=\"" + field.getDBName() + "\" direction=\"desc\"/>");
					}
					pw.println("	</datastore-index>");
				}
			}
			pw.println("</datastore-indexes>");
		}
		context.output.send(ResourceType.WEB_INF, sw.toString(), "datastore-indexes.xml");
	}
}
