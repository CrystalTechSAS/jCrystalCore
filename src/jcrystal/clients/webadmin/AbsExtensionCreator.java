package jcrystal.clients.webadmin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jcrystal.configs.clients.admin.AdminClient;
import jcrystal.configs.clients.admin.ListOption;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.types.IJType;
import jcrystal.types.JType;
import jcrystal.security.SecurityToken;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.Post;
import jcrystal.utils.langAndPlats.JavaCode;

public class AbsExtensionCreator {
	
	public static JavaCode addExtensions(ClientContext context, JCrystalWebServiceManager clase) {
		if(clase.isAdminClient) {
			final IJType entityType = new JType(context.jClassLoader, clase.clase.getAnnotation(AdminClient.class).type());
			final EntityClass entidad = context.data.entidades.get(entityType);
			return new JavaCode() {{
				$("interface ExtAdminClient" + clase.clase.getSimpleName(),()->{
					for(JCrystalWebService ws : clase._metodos)
						if(ws.name().equals("add") || ws.name().equals("list") || ws.name().equals("update") || ws.isAnnotationPresent(ListOption.class)) {
							List<String> results;
							if(ws.name().equals("list")) {
								results = new ArrayList<>();
							}else {
								JCrystalWebServiceParam param = ws.parametros.stream().filter(f->f.type().isAnnotationPresent(Post.class)).findFirst().orElse(null);
								JsonLevel level = param == null ? JsonLevel.MIN : param.type().getAnnotation(Post.class).level();
								results = entidad.manyToOneRelations.stream().filter(f->f.level.level <= level.level && f.editable).map(f->"java.util.List<"+f.type().name()+">").collect(Collectors.toList());
							}
							
							ws.parametros.stream().filter(f->f.type().isAnnotationPresent(jEntity.class) && ! f.type().isAnnotationPresent(Post.class) && !f.type().isSubclassOf(SecurityToken.class) && !f.type().is(entityType)).map(f->"java.util.List<"+f.type().name()+">").forEach(results::add);
							
							String parametros = ws.unwrappedMethod.params.stream().map(f->$(f.type())+" " + f.name()).collect(Collectors.joining(", "));
							if(!results.isEmpty()) {
								if(results.size() == 1)
									$(results.get(0)+" "+ws.name()+"_source("+parametros+");");
								else
									$("jcrystal.results.Tupla"+results.size()+"<"+results.stream().collect(Collectors.joining(", "))+"> "+ws.name()+"_source("+parametros+");");
							}
						}
				});
				if(size()>2) {
					try {
						if(!clase.clase.isSubclassOf(new JType(context.jClassLoader, clase.clase.getPackageName()+".ExtAdminClient" + clase.clase.getSimpleName())))
							add("interface not implemented");
					}catch (Exception e) {
						add("interface not implemented");
					}
				}
			}};
		}
		return null;
	}
	
}
