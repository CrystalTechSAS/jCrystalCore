package jcrystal.model.web;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.ClientId;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.configs.clients.admin.AdminClient;
import jcrystal.clients.utils.JsonWrapper;
import jcrystal.json.JsonBasic;
import jcrystal.json.JsonDetail;
import jcrystal.json.JsonFull;
import jcrystal.json.JsonID;
import jcrystal.json.JsonLevel;
import jcrystal.json.JsonMin;
import jcrystal.json.JsonNormal;
import jcrystal.json.JsonString;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JMethod;
import jcrystal.reflection.MainGenerator;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.Transactional;
import jcrystal.reflection.annotations.async.ClientQueueable;
import jcrystal.reflection.annotations.async.Cron;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Http;
import jcrystal.reflection.annotations.ws.Method;
import jcrystal.reflection.annotations.ws.ResponseHeader;
import jcrystal.reflection.annotations.ws.ResponseHeaders;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.server.FileUploadDescriptor;
import jcrystal.server.async.Async;
import jcrystal.utils.NullComparator;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

/**
* Created by gasotelo on 2/11/17.
*/
public class JCrystalWebService implements IWServiceEndpoint{
	
	public final ClientContext context;
	public final boolean isAdminClient;
	public final JMethod unwrappedMethod;
	public final JCrystalWebServiceManager padre;
	public final List<JCrystalWebServiceParam> parametros = new ArrayList<>();
	private IJType returnType;
	public final JCrystalWebServiceParam tokenParam;
	public boolean hasStringBuider = false;
	private final String rutaMetodo;

	public final ContentType[] contentTypes;
	public Method tipoRuta = Method.GET;
	public Transactional transaccion;
	public boolean external;
	public boolean customResponse = false;
	
	public final Map<IJType, JsonLevel> jsonClassLevels = new TreeMap<>(new NullComparator<>(IJType::compareTo));
	
	public WSStateType stateType;
	
	public List<ResponseHeader> responseHeaders = new ArrayList<>();
	public Map<String, AbsICodeBlock> usageExamples = new TreeMap<>();
	
	public JCrystalWebService(ClientContext context, JCrystalWebServiceManager padre, JMethod m) {
		this.padre = padre;
		this.unwrappedMethod = m;
		this.context = padre.context;
		isAdminClient = padre.clase.isAnnotationPresent(AdminClient.class);
		getJsonLevel();
		
		if(Modifier.isStatic(m.getModifiers()))
			stateType = WSStateType.STATIC;
		else
			stateType = WSStateType.STATELESS;
		
		Http http = m.getAnnotation(Http.class);
		
		transaccion = m.getAnnotation(Transactional.class);
		
		JCrystalWebServiceParam tempToken = null;
		
		//Es importante que esta linea este luego de la anterior debido a las anotaciones que cambian el tipo de solicitud HTTP
		if(!padre.isMultipart && stateType == WSStateType.STATELESS) {
			if(!padre.clase.hasEmptyConstructor())
				padre.clase.constructors.get(0).params.stream().map(p->new JCrystalWebServiceParam(context, this, p, true)).forEach(parametros::add);
			padre.managedAttributes.stream().map(p->new JCrystalWebServiceParam(context, this, p, true)).forEach(parametros::add);
		}
		m.params.stream().map(p->new JCrystalWebServiceParam(context, this, p, false)).forEach(parametros::add);
		
		hasAuthTokenResponse = m.getReturnType().anyMatch(f->context.data.isSecurityToken(f) || f.isAnnotationPresent(LoginResultClass.class));
		
		for (int e = 0; e < parametros.size(); e++) {
			JCrystalWebServiceParam p = parametros.get(e);
			if (tipoRuta.isGetLike() && p.tipoRuta == HttpType.POST)
				tipoRuta = Method.POST;
			if (p.securityToken)
				tempToken = p;
			else if(p.tipoRuta == null);
			else if(hasAuthTokenResponse && p.tipoRuta.isGetLike())
				parametros.get(e).tipoRuta = HttpType.POST;
			
			if(p.type().is(HttpServletResponse.class) || p.type().is(PrintWriter.class)) {
				customResponse = true;
			}
		}
		if(m.isAnnotationPresent(Post.class) || hasAuthTokenResponse)
			tipoRuta = Method.POST;
		
		
		this.tokenParam = tempToken;
		for (int posParam = 0; posParam < parametros.size(); posParam++)
			if (parametros.get(posParam).type().is(StringBuilder.class)) {
				hasStringBuider = true;
			}
		String rutaPadre = padre.getRutaClase().endsWith("/") ? padre.getRutaClase():(padre.getRutaClase() + "/");
		if(!rutaPadre.startsWith("/"))
			rutaPadre = "/" + rutaPadre;
		
		
		if (http != null && !http.path().isEmpty())
			rutaMetodo = http.path();//TODO: posibilida de definir rutas relativas con ./ y ../
		else if (m.getAnnotation(Async.class) != null) {
			rutaMetodo = "/async" + (m.name().equals("index") ? rutaPadre : (rutaPadre  + urlReplacements(m.name()))) + (MainGenerator.JSON_PATHS?".json":"");
			if(parametros.stream().anyMatch(f->f.tipoRuta.isPostLike())) {
				tipoRuta = Method.POST;
				for(JCrystalWebServiceParam param : parametros)
					if(param.tipoRuta == HttpType.GET)
						param.tipoRuta = HttpType.POST;
			}
		}
		else
			rutaMetodo = (m.name().equals("index") ? rutaPadre : (rutaPadre  + urlReplacements(m.name()))) + (MainGenerator.JSON_PATHS?".json":"");
		
		if(m.isAnnotationPresent(Cron.class))
			context.data.CRONS.add(this);
		
		if(padre.isMultipart || m.params.stream().anyMatch(f->f.type().is("FileUploadDescriptor"))){
			contentTypes = new ContentType[] {ContentType.MultipartForm};
			tipoRuta = Method.POST;
		}else if(http != null)
			contentTypes = Arrays.stream(http.content()).map(f->f==ContentType.DEFAULT?ContentType.JSON:f).distinct().toArray(size->new ContentType[size]);
		else
			contentTypes = new ContentType[] {ContentType.JSON};
		
		if(http != null && http.method() != Method.DEFAULT) {
			tipoRuta = http.method();
		}
		
		m.ifAnnotation(ResponseHeader.class, responseHeaders::add);
		m.ifAnnotation(ResponseHeaders.class, h->responseHeaders.addAll(Arrays.asList(h.value())));
	}
	private boolean hasAuthTokenResponse;
	@Override
	public boolean hasAuthTokenResponse() {
		return hasAuthTokenResponse;
	}
	public static String urlReplacements(String url) {
		return url.replace("_", "/").replace("///", "_").replace("//", "-");
	}
	public String exportClientConfigId(ClientGeneratorDescriptor<?> clientConfig) {
		if(clientConfig == null || clientConfig.configs.size() <= 1)
			return "Default";
		
		JAnnotation b = unwrappedMethod.getJAnnotation(clientConfig.annotationClass.name());
		if(b != null)
			return ClientGeneratorDescriptor.getConfigId(b);

		return padre.exportClientConfigId(clientConfig);
	}
	public String getPath(ClientGeneratorDescriptor<?> clientConfig) {
		String id = exportClientConfigId(clientConfig);
		if("Default".equals(id))
			return context.input.SERVER.WEB.servlet_root_path + rutaMetodo; 
		return rutaMetodo;
	}
	public IInternalConfig exportClientConfig(ClientGeneratorDescriptor<?> clientConfig) {
		IInternalConfig config = clientConfig.configs.get(exportClientConfigId(clientConfig));
		if(config!=null)
			return config;
		return clientConfig.configs.values().stream().findFirst().orElse(null);
	}
	
	private void getJsonLevel() {
		final Map<JsonLevel, String[]> jsonLevels = new TreeMap<>();
		if (unwrappedMethod.isAnnotationPresent(JsonFull.class))
			jsonLevels.put(JsonLevel.FULL, unwrappedMethod.getAnnotation(JsonFull.class).value());
		if (unwrappedMethod.isAnnotationPresent(JsonNormal.class))
			jsonLevels.put(JsonLevel.NORMAL, unwrappedMethod.getAnnotation(JsonNormal.class).value());
		if (unwrappedMethod.isAnnotationPresent(JsonBasic.class))
			jsonLevels.put(JsonLevel.BASIC, unwrappedMethod.getAnnotation(JsonBasic.class).value());
		if (unwrappedMethod.isAnnotationPresent(JsonID.class))
			jsonLevels.put(JsonLevel.ID, unwrappedMethod.getAnnotation(JsonID.class).value());
		if (unwrappedMethod.isAnnotationPresent(JsonMin.class))
			jsonLevels.put(JsonLevel.MIN, unwrappedMethod.getAnnotation(JsonMin.class).value());
		if (unwrappedMethod.isAnnotationPresent(JsonDetail.class))
			jsonLevels.put(JsonLevel.DETAIL, unwrappedMethod.getAnnotation(JsonDetail.class).value());
		if (unwrappedMethod.isAnnotationPresent(JsonString.class))
			jsonLevels.put(JsonLevel.TOSTRING, unwrappedMethod.getAnnotation(JsonString.class).value());
		
		for (Map.Entry<JsonLevel, String[]> ent : jsonLevels.entrySet()) {
			if (ent.getValue().length == 0) {
				if (jsonClassLevels.get(null) != null)
					throw new NullPointerException("Ambiguous Json levels");
				else
					jsonClassLevels.put(null, ent.getKey());
			} else
				for (String c : ent.getValue()) {
					IJType jc = context.jClassLoader.forName(c);
					if (jsonClassLevels.get(jc) != null)
						throw new NullPointerException("Ambiguous Json levels for " + c);
					else
						jsonClassLevels.put(jc, ent.getKey());
				}
		}
		if (jsonClassLevels.get(null) == null)
			jsonClassLevels.put(null, JsonLevel.NORMAL);
		
	}
	
	public boolean isJsonArrayResponse(IInternalConfig config) {
		if(unwrappedMethod.returnType.isIterable() && unwrappedMethod.returnType.getInnerTypes().get(0).isAnnotationPresent(jSerializable.class))
			return config.embeddedResponse();
		if(unwrappedMethod.returnType.getSimpleName().startsWith("Tupla"))
			return config.embeddedResponse();
		return false;
	}
	@Override public JsonLevel getJsonLevel(IJType clase) {
		if(clase instanceof JsonWrapper)
			return ((JsonWrapper)clase).level;
		JsonLevel ret = jsonClassLevels.get(clase);
		if (ret == null)
			ret = jsonClassLevels.get(null);
		return ret;
	}
	public final boolean isClientQueueable(){
		return unwrappedMethod.isAnnotationPresent(ClientQueueable.class);
	}
	public String getSubservletMethodName() {
		return (padre.clase.name()+"."+unwrappedMethod.name()).replace(".", "_") + (unwrappedMethod.isAnnotationPresent(Async.class)?"_Async":"");
	}
	@Override public String name() {
		return unwrappedMethod.name();
	}
	public boolean isWritatableResponse() {
		if(unwrappedMethod.isVoid){
			return false;
		}else if(unwrappedMethod.getReturnType().is(FileDownloadDescriptor.class)) {
			return false;
		}
		return true;
	}

	@Override public JCrystalWebServiceParam getTokenParam() {
		return tokenParam;
	}
	@Override public void gatherRequiredTypes(TreeSet<IJType> repetidos){
		for(JCrystalWebServiceParam param : parametros){
			if(param.securityToken)
				repetidos.add(param.type());
			else
				param.p.type().iterate(type->{
					if(type.isJAnnotationPresent(InternalEntityKey.class)) {
						System.out.println(type);
						repetidos.add(type);
					}else if (type.isAnnotationPresent(Post.class)) {
						EntityClass target = context.data.entidades.get(type);
						repetidos.add(type);
						repetidos.add(target.clase);
					}else if(type.isAnnotationPresent(jEntity.class)) {
						EntityClass target = context.data.entidades.get(type);
						if(!target.key.isSimple())
							repetidos.add(type);
					}else if(type.isAnnotationPresent(jSerializable.class) || (type.isEnum() && !type.is(ClientId.class)) || type.isJAnnotationPresent(CrystalDate.class)) {
						repetidos.add(type);
					}
				});
		}
		//Retorno:
		getReturnType().iterate(type->{
			if (type instanceof JsonWrapper) {
				repetidos.add(type);
			}else if (type.isAnnotationPresent(jEntity.class) || type.isAnnotationPresent(jSerializable.class) || type.isEnum())
				repetidos.add(type);
		});
	}
	@Override
	public Method getPathType() {
		return tipoRuta;
	}
	@Override
	public List<JCrystalWebServiceParam> getParameters() {
		return parametros;
	}
	@Override
	public ContentType[] contentType() {
		return contentTypes;
	}
	@Override
	public JCrystalWebServiceManager getManager() {
		return padre;
	}
	@Override
	public IJType getReturnType() {
		if(returnType == null)
			returnType = JsonWrapper.wrap(context, unwrappedMethod.returnType, this);
		return returnType;
	}
	@Override
	public boolean isAdminClient() {
		return isAdminClient;
	}
	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return unwrappedMethod.getAnnotations();
	}
	@Override
	public JAnnotation getJAnnotationWithAncestorCheck(String name) {
		return unwrappedMethod.getJAnnotationWithAncestorCheck(name);
	}
	@Override
	public void registerClientExample(String clientId, AbsICodeBlock example) {
		usageExamples.put(clientId, example);
	}
	
	public boolean hasPostParam() {
		for(JCrystalWebServiceParam param : parametros)
			if(param.tipoRuta != null && param.tipoRuta.isPostLike() && !param.type().is(FileUploadDescriptor.class))
				return true;
		return false;
	}
	@Override
	public Map<String, AbsICodeBlock> getUsageExamples() {
		return usageExamples;
	}
}
