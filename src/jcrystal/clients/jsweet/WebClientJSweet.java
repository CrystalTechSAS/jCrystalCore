package jcrystal.clients.jsweet;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.configs.clients.Client;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.json.JsonLevel;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Method;
import jcrystal.results.Tupla2;
import jcrystal.lang.Language;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.reflection.annotations.*;
import jcrystal.utils.*;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class WebClientJSweet extends AbsClientGenerator<Client>{
	
	public static final String paqueteEntidades = "jcrystal.js.entities";
	public static final String paqueteResultados = "jcrystal.js.results";
	public static final String paqueteControllers = "jcrystal.js.net.controllers";
	public static final String paqueteUtils = "jcrystal.js.utils";
	public static final String paqueteJS = "jcrystal.js";
	public static final String paqueteDates = "jcrystal.js.datetime";
	public static final String paquetePadre = "jcrystal";
	
	public WebClientJSweet(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
		entityGenerator = new GeneradorEntidad(this);
	}
	@Override
	protected void setupEnviroment() throws Exception {
		ContextType.CLIENT.init();
		CodeGeneratorContext.set(Language.JAVASCRIPT, null);
	}
	@Override
	public void generarCliente() throws Exception {
		for(int e=1;e<=5;e++)
		createListener(e);
		for(IJType c : SUPPORTED_NATIVE_TYPES)
			createListener(c);
		
		generarCodigoMobile();
		generarEntidades();
		generarEnums();
		generarResultados();
	}
	private void generarEnums()throws Exception{
		final String paquete = paqueteEntidades+".enums";
		requiredClasses.stream().filter(f->f.isEnum()).forEach(claseEnum->{
			JClass clase = (JClass)claseEnum;
			new JavaCode(){{
				$("package " + paquete + ";");
				$("public enum " + claseEnum.getSimpleName(), ()->{
					try {
						for (EnumValue o : clase.enumData.valores)
							$(o.name + "(" + o.propiedades.get("id") + "),");
						
						$(";");
						$("public final int id;");
						$(claseEnum.getSimpleName() + "(int id)", ()->{
							$("this.id = id;");
						});
						$("public static " + claseEnum.getSimpleName() + " fromId(int id)", ()->{
							$("switch(id)", ()->{
								for (EnumValue o : clase.enumData.valores)
									$("case " + o.propiedades.get("id") + ": return " + o.name + ";");
							});
							$("return null;");
						});
					} catch (Exception ex) {
						throw new NullPointerException(ex.getMessage());
					}
				});
				exportFile(this, paquete.replace(".", File.separator) + File.separator + claseEnum.getSimpleName() + ".java");
			}};
			
		});
		new JavaCode(){{
			$("package " + paquete+";");
			$("public enum JsonLevel", ()->{
				StringSeparator vals = new StringSeparator(", ");
				for(JsonLevel level : JsonLevel.managedValues)
				vals.add(level.name());
				$(vals+";");
			});
			exportFile(this, paquete.replace(".", File.separator) + File.separator + "JsonLevel.java");
		}};
		
	}
	private void generarClaseResultadoOPush(String paquete, final JClass clase){
		final JavaCode cliente = new JavaCode(){{
				$("package "+paquete+";");
				$("import "+paqueteJS+".*;");
				$("import static "+paquetePadre+".JSONUtils.*;");
				$("public class " + clase.getSimpleName(), ()->{
					for(final JVariable f : clase.attributes){
						$("private "+$convert(f.type())+" " + f.name() + ";");
						$("public "+$convert(f.type())+" " + f.name() + "(){return this."+f.name()+";}");
						$("public void " + f.name() + "("+$convert(f.type())+" val){"+f.name()+" = val;}");
					}
					$("public " + clase.getSimpleName() + "()", ()->{});
					$("protected " + clase.getSimpleName() + "(org.json.JSONObject json)throws org.json.JSONException", ()->{
						for(final JVariable f : clase.attributes){
							if(f.type().isEnum())
								requiredClasses.add(f.type());
						}
					});
					generateFromSimpleJson(this, clase, null, clase.attributes);
					generateFromListJson(this, clase, null);
					context.utils.generadorToJson.generateJsonify(clase, this, null, clase.attributes.stream().map(f -> f.accessor()).collect(Collectors.toList()));
					
					if(clase.isAnnotationPresent(LoginResultClass.class)){
						$("private static "+clase.getSimpleName() + " cachedToken = null;");
						$("public " + clase.getSimpleName() + " storeToken()throws org.json.JSONException", ()->{
							$("if(DB"+clase.getSimpleName()+".store(\"Token\", this))cachedToken = this;");
							$("return this;");
						});
						$("public static String getTokenId()", ()->{
							$("if(cachedToken != null)return cachedToken.token;");
							$("else getToken();");
							$("if(cachedToken == null)return null;");
							$("return cachedToken.token;");
						});
						$("public static void deleteToken()", ()->{
							$("cachedToken = null;");
							$("DB"+clase.getSimpleName()+".delete(\"Token\");");
						});
						$("public static " + clase.getSimpleName() + " getToken()", ()->{
							$("if(cachedToken == null)", ()->{
								$("cachedToken = DB"+clase.getSimpleName()+".retrieve(\"Token\");");
							});
							$("return cachedToken;");
						});
						$("public static boolean isAuthenticated()", ()->{
							$("return getToken() != null;");
						});
						if(context.data.rolClass != null){
							for(final EnumValue value : context.data.rolClass.enumData.valores) {
								String name = "Rol" + StringUtils.camelizar(value.name);
								$("public static boolean is"+name+"()", ()->{
									try {
										$("return getToken() != null && (cachedToken.rol & " + value.propiedades.get("id") + ")!=0;");
									} catch (Exception e) {
										$("return false;");
									}
								});
							}
						}
					}
				});
		}};
		//CREAR LA CLASE
		
		exportFile(cliente, paquete.replace(".", File.separator) + File.separator + clase.getSimpleName() + ".java");
	}
	private void generarResultados()throws Exception{
		requiredClasses.stream().filter(f->f.isAnnotationPresent(jSerializable.class)).forEach(clase->{
			generarClaseResultadoOPush(paqueteResultados, (JClass)clase);
		});
	}
	
	private void generarCodigoMobile() throws Exception {
		for(final Map.Entry<String, Set<IWServiceEndpoint>> entry : endpoints.entrySet()){
			String name = "Manager" + StringUtils.capitalize(entry.getKey().contains("/")? entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1) : entry.getKey());
			final String paquete;
			if(entry.getKey().contains("/"))
				paquete = paqueteControllers + "."+entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			else
				paquete = paqueteControllers;
			final JavascriptCode js = new JavascriptCode();
			new JavaCode(){{
					$("package "+paquete+";");
					$("import "+paqueteResultados+".*;");
					$("import "+paqueteEntidades+".enums.*;");
					$("import "+paqueteJS+".*;");
					$("import "+WebClientJSweet.paqueteDates+".*;");
					$("import android.os.AsyncTask;");
					$("public class " + name, ()->{
						for (final IWServiceEndpoint endpoint : entry.getValue()) {
							JCrystalWebService m = (JCrystalWebService)endpoint;
							$("/**");
							$("* " + m.getPath(descriptor));
							$("**/");
							final JavascriptCode interno = new JavascriptCode();
							final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(m);
							processGetParams(this, clientParams.v1.get(HttpType.GET));
							//processPostParams(this, clientParams.v1.get(HttpType.GET));
							
							final List<JVariable> parametros = clientParams.v0;
							
							if(m.unwrappedMethod.isVoid)
								parametros.add(P(GlobalTypes.jCrystal.VoidSuccessListener, "onSuccess"));
							else if(m.getReturnType().is(String.class))
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener(GlobalTypes.STRING),"onSuccess"));
							else if(SUPPORTED_NATIVE_TYPES.contains(m.getReturnType()))
								parametros.add(P(GlobalTypes.jCrystal.NativeSuccessListener(m.getReturnType()),"onSuccess"));
							else if (m.getReturnType().isAnnotationPresent(jEntity.class) || m.getReturnType().isAnnotationPresent(jSerializable.class))
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener($convert(m.getReturnType())) ,"onSuccess"));
							else if (m.getReturnType().isIterable())
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener($convert(m.getReturnType())),"onSuccess"));
							else if(m.getReturnType().getSimpleName().startsWith("Tupla")){
								final List<IJType> tipos = new ArrayList<>(m.getReturnType().getInnerTypes().size());
								for(IJType p : m.getReturnType().getInnerTypes()) {
									if(p.getInnerTypes().isEmpty()) {
										tipos.add($convert(p));
									}else{
										if(p.isSubclassOf(List.class)){
											IJType subClaseTipo = p.getInnerTypes().get(0);
											tipos.add($convert(subClaseTipo).createListType());
										}else
											throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
									}
								}
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener(tipos), "onSuccess"));
							}
							else
								throw new NullPointerException(m.getReturnType().toString());
							parametros.add(P(GlobalTypes.jCrystal.ErrorListener, "onError"));
							$("public static void " + m.name() + "(" + parametros.stream().map(this::$V).map(f->"final " + f).collect(Collectors.joining(", ")) + ")", ()->{});
						}
					});
					context.output.exportFile(this, paquete.replace(".", File.separator) + File.separator + name + ".java");
			}};
			context.output.exportFile(js, paquete.replace(".", File.separator) + File.separator + name + ".js");
		}
	}
	
	private void createListener(int cantidad)throws Exception{
		final String name = "On"+cantidad+"SuccessListener";
		
		StringSeparator classParams = new StringSeparator(',');
		final StringSeparator methodParams = new StringSeparator(',');
		for(int e = 0; e < cantidad; e++){
			String letra = Character.toString((char)('K'+e));
			classParams.add(letra);
			methodParams.add(letra+" "+letra.toLowerCase());
		}
		final JavaCode cliente = new JavaCode(){{
				$("package "+paqueteUtils+";");
				$("public interface " + name+"<"+classParams+">", ()->{
					$("public void onSuccess("+methodParams+");");
				});
		}};
		exportFile(cliente, paqueteUtils.replace(".", File.separator) + File.separator + name + ".java");
	}
	private void createListener(IJType nativetype)throws Exception{
		final String name = "On"+StringUtils.capitalize(nativetype.getSimpleName())+"SuccessListener";
		final JavaCode cliente = new JavaCode(){{
				$("package "+paqueteUtils+";");
				$("public interface " + name, ()->{
					$("public void onSuccess("+nativetype.getSimpleName()+" result);");
				});
		}};
		exportFile(cliente, paqueteUtils.replace(".", File.separator) + File.separator + name + ".java");
	}
	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> parameters) {
		StringSeparator params = new StringSeparator(',');
		for(JVariable param : parameters){
			if(param.type().isPrimitive()){
				params.add("\""+param.name() + "\": " + param.name());
			}else if(param.type().is(String.class)){
				params.add("\""+param.name() + "\": " + param.name());
			}else if(param.type().isPrimitiveObjectType()){
				params.add("\""+param.name() + "\": " + param.name());
			}else if(param.type().getSimpleName().equals("JSONObject")){
				//TODO.
				/*if(param.nombre.equals("body"))
				retorno.add("body");
				else
					retorno.add("body.getJSONObject(\"" + param.nombre + "\")");*/
				throw new NullPointerException();
			}else if(param.type().getSimpleName().equals("JSONArray")){
				//TODO: retorno.add("body.getJSONArray(\"" + param.nombre + "\")");
				throw new NullPointerException();
			}else if(param.type().isJAnnotationPresent(CrystalDate.class)){
				params.add("\""+param.name()+"\": format("+param.name()+")");
			}else if(param.type().isAnnotationPresent(jEntity.class)) {
				EntityClass entidad = context.data.entidades.get(param.type());
				for(EntityField key : entidad.key.getLlaves()) {
					String nombreParam = entidad.key.getLlaves().size()==1 ? ("id"+StringUtils.capitalize(param.name())) : (key.fieldName()+"_"+param.name());
					if(key.type().is(Long.class, long.class)){
						params.add("\""+nombreParam+"\": "+nombreParam);
					}else if(key.type().is(String.class)){
						params.add("\""+nombreParam+"\": "+nombreParam);
					}else
						throw new NullPointerException("Entidad no reconocida 2 "+param.type().getSimpleName());
				}
			}else throw new NullPointerException("Parametro no reconocido "+param.type().getSimpleName());
		}
		if(!parameters.isEmpty() && !params.isEmpty())
			METODO.add("var params = {" + params + "};");
	}
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> parameters) {
		StringSeparator params = new StringSeparator(',');
		METODO.new B(){{
			for(JVariable param : parameters){
				if(param.type().nullable()) {
					METODO.add("if(" + param.name()+" != null){");
					METODO.incLevel();
				}
				if(contentType == ContentType.MultipartForm){
					if(param.type().isPrimitive()){
						$("writer.println(\"-----------------------------\" + boundary);");
						$("writer.append(\"Content-Disposition: form-data; name="+ param.name()+"\\r\\n\");");
						$("writer.append(\"\r\n\");");
						$("writer.println("+param.type().getObjectType().name()+".toString("+param.name()+"));");
					}else if(param.type().is(String.class)){
						$("writer.println(\"-----------------------------\" + boundary);");
						$("writer.append(\"Content-Disposition: form-data; name="+ param.name()+"\\r\\n\");");
						$("writer.append(\"\\r\\n\");");
						$("writer.println(java.net.URLEncoder.encode("+param.name() + ", \"UTF-8\"));");
					}else if(param.type().isPrimitiveObjectType()){
						$("writer.println(\"-----------------------------\" + boundary);");
						$("writer.append(\"Content-Disposition: form-data; name=\""+ param.name()+"\"\r\n\");");
						$("writer.append(\"\r\n\");");
						$("writer.println("+param.type().getObjectType().name()+".toString("+param.name()+"));");
					}else if(param.type().isJAnnotationPresent(CrystalDate.class)){
						$("writer.println(\"-----------------------------\" + boundary);");
						$("writer.append(\"Content-Disposition: form-data; name=\""+ param.name()+"\"\r\n\");");
						$("writer.append(\"\r\n\");");
						$("writer.println("+param.name()+".format());");
					}else
						throw new NullPointerException("Unssuported form data post type " + param.type().name());
				}
				else{
					if(param.type().isAnnotationPresent(Post.class)){
						params.add("\""+param.name()+"\": "+param.name());
					}else if(param.type().isAnnotationPresent(jSerializable.class)){
						params.add("\""+param.name()+"\": "+param.name());
					}
					//TODO: Si se postea una lista de Posts o Jsonifies
					else
						throw new NullPointerException("Unssuported post type " + param.type().name());
				}
				if(param.type().nullable()) {
					METODO.add("}");
					METODO.decLevel();
				}
			}
		}};
		if(!params.isEmpty() || !parameters.isEmpty())
			METODO.add("var body = {};");
	}
	
	
	
	public static void generateFromListJson(AbsCodeBlock bloque, final JClass clase, final JsonLevel level){
		bloque.new B() {{
				$("public static java.util.ArrayList<"+clase.getSimpleName()+(level==null?"":level.baseName())+"> listFromJson"+(level==null?"":level.baseName())+"(org.json.JSONArray json)throws org.json.JSONException", ()->{
					$("java.util.ArrayList<"+clase.getSimpleName()+(level==null?"":level.baseName())+"> ret = new java.util.ArrayList<"+clase.getSimpleName()+(level==null?"":level.baseName())+">(json.length());");
					$("for(int e = 0, i = json.length(); e < i; e++)", ()->{
						$("ret.add(new "+clase.getSimpleName()+"(json.getJSONObject(e)"+(level!=null?", JsonLevel." + level.name():"")+"));");
					});
					$("return ret;");
				});
		}};
	}
	public void generateFromSimpleJson(AbsCodeBlock bloque, final JClass clase, final JsonLevel level, final List<JVariable> campos){
		bloque.new B() {{
				$("public static " + clase.getSimpleName() + " fromJson"+(level==null?"":level.baseName())+"(org.json.JSONObject json)throws org.json.JSONException", ()->{
					if(level == null)
						$("return new " + clase.getSimpleName() + "(json);");
					else {
						$(clase.getSimpleName() + " ret = new " + clase.getSimpleName() + "();");
						$("return ret;");
					}
				});
		}};
	}
	
	
}
