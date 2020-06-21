package jcrystal.main.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.configs.server.dbs.DBInstance;
import jcrystal.datetime.AbsCrystalDate;
import jcrystal.datetime.DateType;
import jcrystal.entity.types.CreationTimestamp;
import jcrystal.entity.types.ModificationTimestamp;
import jcrystal.model.security.SecurityTokenClass;
import jcrystal.model.server.SerializableClass;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.model.web.QueueDescriptor;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JMethod;
import jcrystal.types.JType;
import jcrystal.utils.StringUtils;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.jEntityImpl;
import jcrystal.reflection.annotations.EntidadPush;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.EntityProperty;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.RolEnum;
import jcrystal.reflection.annotations.async.Queue;
import jcrystal.reflection.annotations.async.Queues;
import jcrystal.security.SecurityToken;

public class ClientData {
	public final DataEntidades entidades;
	public final DataBackends BACKENDS;
	
	public TreeMap<String, IJType> map_clases;
	
	public JClass rolClass = null;
	public final List<JClass> clases_controladores = new ArrayList<>();
	public final List<JClass> clases_push_entities = new ArrayList<>();
	public final Map<String, SerializableClass> serializableClasses = new TreeMap<>();
	public final Map<String, JClass> auth_builders = new TreeMap<>();
	public final Map<String, QueueDescriptor> queues = new TreeMap<>();
	public final List<JCrystalWebService> CRONS = new ArrayList<>();
	public final Map<String, DBInstance> databases = new TreeMap<>(Comparator.nullsFirst(String::compareTo));
	
	public ArrayList<JMethod> globalServerLoadMethods = new ArrayList<>();
	
	public final Map<IJType, SecurityTokenClass> tokens = new TreeMap<>();
	
	public final List<JCrystalWebServiceManager> services = new ArrayList<>();
	
	private final List<JClass> entityClasses = new ArrayList<>();
	
	private static final Map<String, DateType> dateTypes = Arrays.stream(DateType.values()).collect(Collectors.toMap(f->"Crystal"+StringUtils.camelizar(f.name()), f->f));
	
	public final AdminData adminData = new AdminData();
	
	public ClientData(ClientInput input) {
		BACKENDS = new DataBackends(input);
		entidades = new DataEntidades();
		map_clases = input.jClassResolver.getLoadedClasses();

		for (IJType clase : map_clases.values()) {
			if(addExtraData(clase));
			else if(clase.isAnnotationPresent(Post.class));
			else if(clase instanceof JClass) {
				if(JCrystalWebServiceManager.isManager(clase))
					clases_controladores.add((JClass)clase);
				else if (clase.isAnnotationPresent(jSerializable.class))
					serializableClasses.put(clase.name(), new SerializableClass((JClass)clase));
				else if (clase.isAnnotationPresent(jEntity.class))
					entityClasses.add((JClass)clase);
				else if (clase.isAnnotationPresent(EntidadPush.class))
					clases_push_entities.add((JClass)clase);
				else if (clase.isAnnotationPresent(RolEnum.class)){
					if(rolClass != null)
						throw new NullPointerException("You can have just one rol enum: " + clase.name() + " _ " + rolClass.name());
					rolClass = (JClass)clase;
				}
				else if(clase instanceof JClass && ((JClass)clase).interfaces.stream().anyMatch(f->f.getSimpleName().endsWith("AuthBuilder"))) {
					IJType iface = ((JClass)clase).interfaces.stream().filter(f->f.getSimpleName().endsWith("AuthBuilder")).findFirst().orElse(null);
					auth_builders.put(iface.getSimpleName().substring(0, iface.getSimpleName().length() - "AuthBuilder".length()), (JClass)clase);
				}
				else if(clase.isSubclassOf(MaskedEnum.class))
					BACKENDS.MAIN.enums.add((JClass)clase);
				
				if (clase.isSubclassOf(SecurityToken.class))
					tokens.put(clase, new SecurityTokenClass((JClass)clase));
				
			}
			if(clase.isAnnotationPresent(Queue.class)) {
				QueueDescriptor desc = new QueueDescriptor(clase.getAnnotation(Queue.class));
				queues.put(desc.userName, desc);
			}
			if(clase.isAnnotationPresent(Queues.class))
				Arrays.stream(clase.getAnnotation(Queues.class).value()).map(f->new QueueDescriptor(f)).forEach(f->queues.put(f.userName, f));
		}
		for(DBInstance db : input.SERVER.DB.list)
			if(db != input.SERVER.DB.MAIN)
				databases.put(db.id, db);
		input.SERVER.DB.MAIN.id = null;
		databases.put(null, input.SERVER.DB.MAIN);
	}
	private boolean addExtraData(IJType type) {
		if(type.name().startsWith("jcrystal.datetime") && type.isSubclassOf(AbsCrystalDate.class)) {
			JType temp = (JType)type;
			temp.addAnnotation(new jcrystal.reflection.annotations.CrystalDate(dateTypes.get(temp.getSimpleName())));
			return true;
		}
		else if(type.is(CreationTimestamp.class, ModificationTimestamp.class)) {
			((JType)type).addAnnotation(new jcrystal.reflection.annotations.CrystalDate(DateType.DATE_MILIS));
			return true;
		}else if(type instanceof JClass) {
			JClass clase = (JClass)type;
			if(!clase.isAnnotationPresent(jEntity.class) && clase.attributes.stream().anyMatch(f->f.isAnnotationPresent(EntityProperty.class) || f.isAnnotationPresent(EntityKey.class))) {
				clase.addAnnotation(new jEntityImpl());
			}
			if(clase.getSimpleName().equals("Key") && clase.declaringClass != null && clase.declaringClass.isAnnotationPresent(jEntity.class)) {
				clase.addAnnotation(new InternalEntityKey(clase.declaringClass.getSimpleName()+".Key", clase.declaringClass));
			}
		}
		return false;
	}
	public void doPreprocess(ClientContext context) {
		System.out.println("doPreprocess");
		entidades.doPreprocess(entityClasses, context);
		clases_controladores.stream().map(f->new JCrystalWebServiceManager(context, f)).forEach(services::add);
	}
	public boolean isSecurityToken(IJType type) {
		EntityClass entity = entidades.get(type);
		if(entity != null)
			return entity.isSecurityToken();
		return tokens.containsKey(type);
	}
	public static class AdminData{
		public JCrystalWebService login, logout;
		public IJType tokenType;
	}
}
