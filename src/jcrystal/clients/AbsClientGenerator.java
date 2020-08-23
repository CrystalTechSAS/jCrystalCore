package jcrystal.clients;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.utils.EntityUtils;
import jcrystal.clients.utils.JsonWrapper;
import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.configs.clients.SuccessType;
import jcrystal.db.query.Page;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.Password;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalMultipartWebService;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JClass;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.preprocess.responses.OutputFile;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.results.Tupla2;
import jcrystal.service.types.Authorization;
import jcrystal.utils.DefaultTreeMap;
import jcrystal.utils.HashUtils;
import jcrystal.utils.InternalException;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.HTMLCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
* Created by AndreaC on 17/12/2016.
*/
public abstract class AbsClientGenerator<T extends Client> {
	
	public static TreeSet<IJType> SUPPORTED_NATIVE_TYPES = new TreeSet<>(Arrays.asList(GlobalTypes.INT, GlobalTypes.DOUBLE, GlobalTypes.LONG, GlobalTypes.BOOLEAN));
	
	public final ClientGeneratorDescriptor<T> descriptor;
	public final ClientContext context;
	public final EntityUtils entityUtils;
	
	protected TreeMap<String, Set<IWServiceEndpoint>> endpoints = new DefaultTreeMap<>(f->new TreeSet<>());
	public TreeSet<IJType> requiredClasses = new TreeSet<>();
	protected AbsEntityGenerator<?> entityGenerator;
	
	private String outputPath = "";
	
	public AbsClientGenerator(ClientGeneratorDescriptor<T> descriptor){
		this.descriptor = descriptor;
		this.context = descriptor.context;
		this.entityUtils = context.utils.getEntityUtils();
	}
	public AbsClientGenerator<T> setOutputPath(String outputPath) {
		this.outputPath = outputPath;
		return this;
	}
	public RequieredEntities getRequiredEntities() {
		RequieredEntities ret = new RequieredEntities();
		
		requiredClasses.stream().filter(f->f.isAnnotationPresent(Post.class)).forEach(post->{
			EntityClass entidad = context.data.entidades.get(post);
			Post data = post.getAnnotation(Post.class);
			if(entidad.getUsedLevels().contains(data.level()))
				ret.complete.get(entidad.clase).add(data.level());
		});
		requiredClasses.stream().filter(f->f.isAnnotationPresent(jEntity.class)).forEach(entity->{
			JsonLevel level = entity instanceof JsonWrapper ? ((JsonWrapper)entity).level : null;
			EntityClass entidad = context.data.entidades.get(context.data.entidades.targetEntity(entity));
			if(level!=null) {
				if(entidad.getUsedLevels().contains(level)) {
					ret.complete.get(context.data.entidades.targetEntity(entity)).add(level);
					entidad.fields.stream().filter(f->f.getTargetEntity() != null && ( f.level == null || f.level.level <= level.level )).forEach(f->{
						if(f.getTargetEntity().key == null)
							ret.complete.get(f.getTargetEntity().clase).add(level);
						else
							ret.addKey(f.getTargetEntity());
					});
					for(EntityClass padre = entidad; padre != null; padre = padre.padre)
						if(padre.getUsedLevels().contains(level))
							ret.complete.get(padre.clase).add(level);
				}
			}else
				ret.addKey(entidad);
		});
		ret.clear();
		return ret;
	}
	public boolean registrar(JCrystalWebServiceManager controller, JCrystalWebService metodo){
		JAnnotation annotation = null;
		if(controller.isMultipart) {
			if((annotation = controller.clase.getJAnnotationWithAncestorCheck(descriptor.clientAnnotationClass.name())) != null) {
				Set<IWServiceEndpoint> metodosMobile = endpoints.get(controller.getRutaClase());
				IWServiceEndpoint endpoint = new JCrystalMultipartWebService(context, controller);
				metodosMobile.add(endpoint);
				endpoint.gatherRequiredTypes(requiredClasses);
				return true;
			}
		}else if((annotation = metodo.getJAnnotationWithAncestorCheck(descriptor.clientAnnotationClass.name())) != null){
			Set<IWServiceEndpoint> metodosMobile = endpoints.get(controller.getRutaClase());
			metodosMobile.add(metodo);
			IInternalConfig config =  descriptor.configs.get(ClientGeneratorDescriptor.getConfigId(annotation));
			if(config != null)
				metodo.external |= config.external();
			metodo.gatherRequiredTypes(requiredClasses);
			return true;
		}
		return false;
	}
	
	public void generar() throws Exception {
		setupEnviroment();
		generarCliente();
		CodeGeneratorContext.clear();
	}
	protected abstract void setupEnviroment() throws Exception;
	protected abstract void generarCliente() throws Exception;
	
	public final void generarEntidades(){
		if(entityGenerator != null) {
			RequieredEntities entities = getRequiredEntities();
			for(final Map.Entry<IJType, TreeSet<JsonLevel>> classEntity : entities.complete.entrySet()){
				final EntityClass entidad = context.data.entidades.get(classEntity.getKey());
					entityGenerator.generateEntity(entidad, classEntity.getValue());
					if(entityGenerator.entityValidatorGenerator != null)
						entityGenerator.entityValidatorGenerator.create(entidad, classEntity.getValue());
			}
			for(EntityClass entityKey : entities.keysOnly)
				entityGenerator.generateEntityKey(entityKey);
		}
	}
	
	
	public void addResource(InputStream resource, String path) throws Exception{
		StringWriter sw = new StringWriter();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(resource)); PrintWriter pw = new PrintWriter(sw)){
			for(String line; (line = br.readLine())!=null; )
				pw.println(line);
		}
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, path, sw.toString()));
	}
	public void addResource(List<String> resource, String path){
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, path, resource));
	}
	
	public void addResource(InputStream resource, String paquete, String path) throws Exception{
		StringWriter sw = new StringWriter();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(resource)); PrintWriter pw = new PrintWriter(sw)){
			pw.println("package "+paquete+";");
			for(String line; (line = br.readLine())!=null; )
				pw.println(line);
		}
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, path, sw.toString()));
	}
	public void addResource(InputStream resource, Map<String, Object> config, String path) throws Exception{
		StringWriter sw = new StringWriter();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(resource)){
	        	public String readLine() throws IOException {
	        		String line = super.readLine();
	        		if(line != null)
	        			for(Entry<String, Object> val : config.entrySet())
	                		line = line.replace("#"+val.getKey(), ""+val.getValue());
	                return line;
	        	};
	        }; PrintWriter pw = new PrintWriter(sw)){
	        	for(String line; (line = br.readLine())!=null; ){
	        		if(line.startsWith("#IF")){
	            		final Boolean val = (Boolean)config.get(line.substring(3).trim());
	            		ArrayList<String> IF = new ArrayList<>();
	            		ArrayList<String> ELSE = new ArrayList<>();
	            		for(;!(line=br.readLine()).startsWith("#");IF.add(line));
	            		if(line.startsWith("#ELSE"))
	            			for(;!(line=br.readLine()).startsWith("#");ELSE.add(line));
	            		if(!line.startsWith("#ENDIF"))
	            			throw new NullPointerException();
	            		(val?IF:ELSE).stream().forEach(pw::println);
	            	}else 
	            		pw.println(line);
	            }
		}
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, outputPath + path, sw.toString()));
	}
	public void exportFile(AbsCodeBlock codigo, String path){
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, outputPath + path, codigo.getCode()));
	}
	public void exportFile(HTMLCode codigo, String path){
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, outputPath + path, codigo.getCode()));
	}
	public void exportFile(String codigo, String path){
		context.output.send(new OutputFile(descriptor.client.id, descriptor.client.type, outputPath + path, codigo));
	}

	protected abstract <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params);
	protected abstract <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params);
	
	protected IJType getReturnCallbackType(AbsCodeBlock code, IWServiceEndpoint m) {
		if (m.getReturnType().is(Void.TYPE))
			return GlobalTypes.jCrystal.VoidSuccessListener;// "() => void"
		else if (m.getReturnType().isTupla()) {
			return GlobalTypes.jCrystal.OnSuccessListener(m.getReturnType().getInnerTypes().stream().map(p->code.$convert(p)).collect(Collectors.toList()));
		}else if(m.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class, File.class))
			return GlobalTypes.jCrystal.OnSuccessListener(m.getReturnType());
		else {
			return GlobalTypes.jCrystal.OnSuccessListener(code.$convert(m.getReturnType()));
		}
	}
	
	protected Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> getParamsWebService(IWServiceEndpoint m) {
		List<JVariable> params = new ArrayList<>();
		Map<HttpType, List<JVariable>> ret = new TreeMap<HttpType, List<JVariable>>();
		for(HttpType type : HttpType.values())
			ret.put(type, new ArrayList<JVariable>() {
				@Override
				public boolean add(JVariable e) {
					if(e.value == null)
						params.add(e);
					return super.add(e);
				}
			});
		if(m.getReturnType().is(Page.class))
			ret.get(HttpType.HEADER).add(new JVariable(GlobalTypes.STRING, "nextToken"+HashUtils.shortMD5(m.getPath(null)), "nextPageToken"));
		for (JCrystalWebServiceParam param : m.getParameters()) {
			switch (param.tipoRuta) {
				case SERVER:
					break;
				case SESSION:
					break;
				case PATH:
					if (param.p.type().is(String.class))
						ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.STRING, param.nombre));
					else if (param.p.type().isPrimitive())
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					else
						throw new InternalException(500, "Unssupported type for path type " + param.tipoRuta + " " + param.p.type() + ":" + param.nombre);
					break;
				case HEADER:
					if (param.p.type().is(Authorization.class))
						ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.STRING, param.nombre));
					else
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					break;
				case POST:
					if(param.p.type().is("jcrystal.server.FileUploadDescriptor"))
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.name()));
					else if (param.p.type().is(String.class, LongText.class)) {
						ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.STRING, param.nombre));
					} else if (param.p.type().isJAnnotationPresent(CrystalDate.class)) {
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().isPrimitive() || param.p.type().isPrimitiveObjectType()) {
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().isAnnotationPresent(Post.class) || param.p.type().isJAnnotationPresent(InternalEntityKey.class)) {
						ret.get(param.tipoRuta).add(new JVariable(param.type(), param.nombre));
					} else if(param.p.type().isAnnotationPresent(jEntity.class)) {
						EntityClass entidad = context.data.entidades.get(param.p.type());
						if(m.hasAuthTokenResponse() && entidad.hasAccountFields) {
							entidad.iterateKeysAndProperties().filter(f->f.isAccountField()||f.type().is(Password.class)).forEach(campo->{
								ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.STRING, campo.fieldName()));
							});
						}else if(entidad.key.isSimple()) {
							String nombre = "id"+StringUtils.capitalize(param.nombre);
							ret.get(param.tipoRuta).add(new JVariable(entidad.key.getSingleKeyType(), nombre));
						}else
							ret.get(param.tipoRuta).add(new JVariable(entidad.key.getSingleKeyType(), param.nombre));
					} else if (param.p.type().isAnnotationPresent(jSerializable.class)) {
						requiredClasses.add(param.p.type());
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().isIterable()) {
						final IJType tipo = param.p.type().getInnerTypes().get(0);
						if (tipo.is(String.class, Long.class, Integer.class))
							ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
						else if (tipo.isAnyAnnotationPresent(jSerializable.class, Post.class)) {
							ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
						}else if (tipo.isAnnotationPresent(jEntity.class)) {
							EntityClass entidad = context.data.entidades.get(tipo);
							ret.get(param.tipoRuta).add(new JVariable(entidad.key.getSingleKeyType().createListType(), param.nombre));
						}else
							throw new NullPointerException("Unssuported post type " + param.p.type().name()); // TODO: Si se postea una lista de Posts o Jsonifies
					} else if (param.p.type().is(org.json.JSONObject.class)) {
						ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.Json.JSONObject, param.nombre));
					}else if (param.p.type().isArray()) {
						IJType arrayType = param.p.type().getInnerTypes().get(0);
						if(arrayType.isPrimitive())
							ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
						else
							throw new NullPointerException("Unssuported post type " + arrayType.name());
					} else
						throw new NullPointerException("Unssuported post type " + param.p.type().name());
					break;
				case GET:
					if (param.p.type().isPrimitive()) {
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().is(String.class)) {
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().isPrimitiveObjectType()) {
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					}
					else if (param.p.type().isEnum()) {
						requiredClasses.add(param.p.type());
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().getSimpleName().equals("JSONObject")) {
						// TODO.
						/*
						 * if(param.nombre.equals("body")) ret.get(param.tipoRuta).add("body"); else
						 * ret.get(param.tipoRuta).add("body.getJSONObject(\"" + param.nombre + "\")");
						 */
						throw new NullPointerException();
					} else if (param.p.type().getSimpleName().equals("JSONArray")) {
						// TODO: ret.get(param.tipoRuta).add("body.getJSONArray(\"" + param.nombre + "\")");
						throw new NullPointerException();
					} else if (param.p.type().isJAnnotationPresent(CrystalDate.class)) {
						ret.get(param.tipoRuta).add(new JVariable(param.p.type(), param.nombre));
					} else if (param.p.type().isAnnotationPresent(jEntity.class)) {
						EntityClass entidad = context.data.entidades.get(param.p.type());
						if(entidad.key.isSimple()) {
							EntityField key = entidad.key.getLast();
							String nombreParam = "id" + StringUtils.capitalize(param.nombre);
							if (key.type().is(Long.class, long.class)) {
								if (param.required) {
									ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.LONG, nombreParam));
								} else {
									ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.OBJ_LONG, nombreParam));
								}
							} else if (key.type().is(String.class)) {
								ret.get(param.tipoRuta).add(new JVariable(GlobalTypes.STRING, nombreParam));
							} else
								throw new NullPointerException("Entidad no reconocida 2 " + param.p.type().getSimpleName());
						}else {
							throw new InternalException(500, "Can't send a complex entity key as query param");
						}
					} else
						throw new NullPointerException("Parametro no reconocido " + param.p.type().getSimpleName());
				break;
			}
		}
		return new Tupla2<>(params, ret);
	}
	
	public IJType getSuccessType(SuccessType type) {
		switch (type) {
		case INT:
			return GlobalTypes.INT;
		default:
			break;
		}
		throw new NullPointerException("Unssuported success type " + type);
	}
	protected static class Utils{
		public static String getManagerName(String path) {
			return "Manager" + StringUtils.capitalize(path.contains("/")? path.substring(path.lastIndexOf('/') + 1) : path);
		}
	}
	public class RequieredEntities{
		public final TreeMap<IJType, TreeSet<JsonLevel>> complete = new TreeMap<IJType, TreeSet<JsonLevel>>() {
			private static final long serialVersionUID = -1237269355286645400L;
			@Override public TreeSet<JsonLevel> get(Object key) {
				TreeSet<JsonLevel> ret = super.get(key);
				if(ret == null)
					put((IJType)key, ret = new TreeSet<JsonLevel>(JsonLevel.nullComparator));
				return ret;
			}
		};
		public final TreeSet<EntityClass> keysOnly = new TreeSet<EntityClass>();
		void clear() {
			for(final IJType classEntity : complete.keySet()){
				final EntityClass entidad = context.data.entidades.get(classEntity);
				keysOnly.remove(entidad);
			}
		}
		void addKey(EntityClass entity) {
			if(!entity.key.isSimple()) {
				keysOnly.add(entity);
				entity.key.stream().filter(f->f.getTargetEntity() != null).forEach(f->addKey(f.getTargetEntity()));
			}
		}
	}
}
