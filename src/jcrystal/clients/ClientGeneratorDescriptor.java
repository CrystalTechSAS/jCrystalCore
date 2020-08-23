package jcrystal.clients;

import java.util.Map;
import java.util.TreeMap;

import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.ClientType;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.configs.clients.admin.AdminClient;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.entities.CarbonCopy;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JType;
import jcrystal.types.JVariable;
import jcrystal.utils.StringUtils;

public class ClientGeneratorDescriptor<T extends Client> {
	
	public final Map<String, FieldTreeMap> fieldsClient = new TreeMap<>();
	
	public final T client;
	
	public Map<String, IInternalConfig> configs = new TreeMap<>();
	
	public IJType clientAnnotationClass;
	
	protected final ClientContext context;
	
	public ClientGeneratorDescriptor(ClientContext context, T client, IJType annotationClass) {
		this.context = context;
		this.client = client;
		client.configs.stream().forEach(f->configs.put(f.id()==null?"Default":f.id(), f));
		this.clientAnnotationClass = annotationClass;
	}

	public ClientGeneratorDescriptor(ClientContext context, T client) {
		super();
		this.context = context;
		this.client = client;
		if(client!=null) {
			client.configs.stream().forEach(f->configs.put(f.id()==null?"Default":f.id(), f));
			try {
				if(client.type == ClientType.ADMIN)
					this.clientAnnotationClass = context.jClassLoader.load(AdminClient.class, null);
				else
					this.clientAnnotationClass = new JType(context.jClassLoader, "jcrystal.clients.Client"+StringUtils.capitalize(client.id));
				
			} catch (Exception e) {
				this.clientAnnotationClass = null;
				e.printStackTrace();
			}
		}
	}
	public FieldTreeMap getFields(EntityClass entidad) {
		if(fieldsClient.containsKey(entidad.clase.name())) {
			return fieldsClient.get(entidad.clase.name());
		}else {
			FieldTreeMap levels = new FieldTreeMap();
			for(EntityField f : entidad.fields){
				if(f.level != null && f.level.autoManaged && !f.isConstant){
					JsonLevel annotatedLevel = getLevel(f.f);
					if(annotatedLevel != JsonLevel.DEFAULT) {
						if(annotatedLevel != JsonLevel.NONE)
							levels.add(annotatedLevel, entidad, f);
					}
					else if(f.keyData == null)
						levels.add(f.level, entidad, f);
				}else if(f.level == null && f.isJAnnotationPresent(this.clientAnnotationClass)) {
					levels.add(null, entidad, f);
				}
			};
			//TODO: N6 potencial de inseguridad debido a esta clausula: clase.clase.isAnnotationPresent(LoginResultClass.class)
			//TODO: Se diseñó sin esa clausula para evitar que un usuario pueda ver el token de otro.
			//if(!entidad.isSecurityToken() && !entidad.clase.isAnnotationPresent(LoginResultClass.class))
			if(entidad.key != null)
				for(EntityField f : entidad.key.getLlaves())
					if(!f.isConstant)
						levels.add(JsonLevel.ID, entidad, f);
			
			//Padre
			if(entidad.padre != null)
				levels.mergeFrom(getFields(entidad.padre));
			
			fieldsClient.put(entidad.clase.name(), levels);
			return levels;
		}
	}
	public static String getConfigId(JAnnotation annotation) {
		if(!annotation.values.containsKey("type"))
			return "Default";
		return (String)annotation.values.get("type");
	}
	public JsonLevel getLevel(JVariable f){
		if(client != null && context.jClassLoader.getLoadedClasses().containsKey("jcrystal.clients.ClientLevel") && f.isJAnnotationPresent("jcrystal.clients.ClientLevel")) {
			return JsonLevel.valueOf((String) f.getJAnnotation("jcrystal.clients.ClientLevel").values.get(client.id));
		}
		return JsonLevel.DEFAULT;
	}

}
