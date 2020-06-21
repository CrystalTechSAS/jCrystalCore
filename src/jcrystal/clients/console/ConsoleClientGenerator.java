package jcrystal.clients.console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.configs.clients.Client;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.docs.Doc;
import jcrystal.reflection.docs.Param;
import jcrystal.reflection.docs.Params;
import jcrystal.results.Tupla2;
import jcrystal.types.JVariable;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;

public class ConsoleClientGenerator extends AbsClientGenerator<Client>{
	
	public static final String rutas = "rutas.html";
	public static final String paqueteControllers = "jcrystal.mobile.net.controllers";
	public ConsoleClientGenerator(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
	}
	@Override
	protected void setupEnviroment() throws Exception {
		ContextType.CLIENT.init();
	}
	@Override
	public void generarCliente() throws Exception {
		addResource(paqueteControllers, "HTTPUtils");
		addResource(paqueteControllers, "DateType");
		addResource(paqueteControllers, "MyReader");
		generarCodigoMobile();
	}
	
	private void addResource(String paquete, String name) throws Exception{
		addResource(ConsoleClientGenerator.class.getResourceAsStream("net/" + name), paquete, paquete.replace(".", File.separator) + File.separator + name + ".java");
	}
	
	private void generarCodigoMobile() throws Exception {
		final List<String> opciones = new ArrayList<>();
		final List<String> opcionesDoc = new ArrayList<>();
		for(final Map.Entry<String, Set<IWServiceEndpoint>> entry : endpoints.entrySet()){
			String name = "Manager" + StringUtils.capitalize(entry.getKey().contains("/")? entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1) : entry.getKey());
			final String paquete;
			if(entry.getKey().contains("/"))
				paquete = paqueteControllers + "."+entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			else
				paquete = paqueteControllers;
			final JavaCode cliente = new JavaCode(){{
					$("package "+paquete+";");
					$("import "+paqueteControllers+".*;");
					$("public class " + name, ()->{
						for (final IWServiceEndpoint endpoint : entry.getValue()) {
							JCrystalWebService m = (JCrystalWebService)endpoint;
							final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(m);
							final List<JVariable> parametros = clientParams.v0;
							opciones.add(paquete+"."+name+"."+m.name());
							$("public static void " + m.name() + "(MyReader reader)", ()->{
								{
									$("try", ()->{
										String url = m.exportClientConfig(descriptor).BASE_URL(null);
										$("String ruta = \"" + url + m.getPath(descriptor) + "\";");
										processGetParams(this, clientParams.v1.get(HttpType.GET));
										$("System.out.println(\"GET \"+ruta);");
										processPostParams(this, m.contentTypes[0], clientParams.v1.get(HttpType.POST));
										$("java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(ruta).openConnection();");
										$("connection.setConnectTimeout(150000);");
										$("connection.setRequestMethod(\"GET\");");
										$("connection.setRequestProperty(\"Accept\", \"application/json\");");
										if (m.tokenParam != null) {
											$("connection.setRequestProperty(\"Authorization\", "+m.tokenParam.type().getSimpleName()+".getTokenId());");
										}
										$("connection.connect();");
										$("final int responseCode = connection.getResponseCode();");
										$if("responseCode == 200", ()->{
											$("final StringBuilder resp = HTTPUtils.readResponse(connection.getInputStream());");
											$("connection.disconnect();");
											$("System.out.println(resp);");
										});
										$else(()->{
											$("final StringBuilder resp = HTTPUtils.readResponse(connection.getErrorStream());");
											$("connection.disconnect();");
											$("System.out.println(resp);");
										});
									});
									$("catch (Exception ex)", ()->{
										$("ex.printStackTrace();");
									});
								}
							});
							
							if(m.getAnnotation(Doc.class)!=null){
								$("public static String "+ m.name()+"Doc()", ()->{
									$("String s =  \"<h3> /**"+m.getAnnotation(Doc.class).value()+"**/</h3>\";");
									$("s+=\"<table>\";");
									$("s +=\"<tr><td>Ruta</td><td>"+m.getPath(descriptor)+"</td></tr>\";");
									$("System.out.println(\"Consultando servicio " + m.getPath(descriptor) + "...\");");
									if(m.getAnnotation(Params.class)!=null){
										$("s+=\"<tr><td>Parámetros</td><td>\";");
										for(Param p : m.getAnnotation(Params.class).value()){
											$("s+=\""+p.name()+"="+p.value()+"<br>\";");
										}
										$("s+=\"</td></tr>\";");
									}
									$("try", ()->{
										
										
										String url = m.exportClientConfig(descriptor).BASE_URL(null);
										$("String ruta = \"" + url + m.getPath(descriptor) + "\";");
										if(m.getAnnotation(Params.class)!=null){
											$("String params = null;");
											for(Param p : m.getAnnotation(Params.class).value()){
												$("	params = (params==null?\"?\":(params + \"&\")) + \""+p.name()+"="+p.test()+"\";");
											}
											$("ruta+=params;");
										}
										$("s+=\"<tr><td>Ejemplo:<br>GET \"+ruta+\"</td>\";");
										$("java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(ruta).openConnection();");
										$("connection.setConnectTimeout(150000);");
										$("connection.setRequestMethod(\"GET\");");
										$("connection.setRequestProperty(\"Accept\", \"application/json\");");
										if (m.tokenParam != null) {
											$("connection.setRequestProperty(\"Authorization\", "+m.tokenParam.type().getSimpleName()+".getTokenId());");
										}
										$("connection.connect();");
										$("final int responseCode = connection.getResponseCode();");
										$if("responseCode == 200", ()->{
											$("final StringBuilder resp = HTTPUtils.readResponse(connection.getInputStream());");
											$("connection.disconnect();");
											$("s+=\"<td>Respuesta:<br>\";");
											$("s+=resp+\"</td>\";");
										});
										$else(()->{
											$("final StringBuilder resp = HTTPUtils.readResponse(connection.getErrorStream());");
											$("connection.disconnect();");
											
											$("System.out.println(resp);");
											$("s+=\"<td>¡Error!</td>\";");
										});
										$("s+=\"</tr>\";");
									});
									$("catch (Exception ex)", ()->{
										$("ex.printStackTrace();");
									});
									$("s+=\"</table><br><br>\";");
									$("return s;");
								});
								opcionesDoc.add(paquete+"."+name+"."+ m.name()+"Doc()");
							}
						}
					});
			}};
			exportFile(cliente, paquete.replace(".", File.separator) + File.separator + name + ".java");
		}
		generateDocumentation(opcionesDoc);
		final JavaCode main = new JavaCode(){{
				$("package "+paqueteControllers+";");
				$("public class Main", ()->{
					$("public static void main(String...args)", ()->{
						$("MyReader reader = new MyReader();");
						$("while(true)", ()->{
							$("System.out.println(\"---/---/---\");");
							for(int e = 0; e < opciones.size(); e++)
							$("System.out.println(\""+e+": " + opciones.get(e) + "\");");
							$("System.out.println(\"-10 Documentación\");");
							$("switch(reader.readInt())", ()->{
								// TODO Auto-generated method stub
								for(int e = 0; e < opciones.size(); e++){
									final String caso = opciones.get(e);
									$("case "+e+":", ()->{
										$(caso + "(reader);");
									});
									$("break;");
								}
								$("case -10:", ()->{
									$("System.out.println(\"Escriba la ruta absoluta donde desea la documentación:\");");
									$(paqueteControllers+".DocumentationGen.writeDocumentation(reader);");
									$("break;");
								});
								$("case -1:", ()->{
									$("return;");
								});
							});
						});
					});
				});
		}};
		exportFile(main, paqueteControllers.replace(".", File.separator) + File.separator + "Main.java");
	}
	
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		if(params.isEmpty())
			return;
		if(contentType == ContentType.JSON)
			METODO.$("org.json.JSONObject body = new org.json.JSONObject();");
	}

	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params) {
		if(params.isEmpty())
			return;
		METODO.new B(){{
			$("String params = null;");
			for(JVariable param : params){
				$("System.out.println(\"Inserte " + param.name() + " : " + param.type() + "\");");
				if(param.type().isJAnnotationPresent(CrystalDate.class))
					$("System.out.println(" + param.type().getSimpleName() + ".FORMAT);");
				$(param.type().getSimpleName()+" " + param.name() + " = reader.readLine();");
				if(param.type().nullable()) {
					$("if(" + param.name() + "!= null && !"+param.name()+".isEmpty()){");
					METODO.incLevel();
				}
				if(param.type().isPrimitive()){
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name()+"=\" + "+ param.type().getObjectType().name()+".toString(reader.read"+StringUtils.capitalize(param.type().getSimpleName())+"());");
				}else if(param.type().isPrimitiveObjectType()){
					$("params = (params==null?\"?\":(params + \"&\")) + \""+param.name()+"=\" + " + param.type().getObjectType().name()+".toString("+param.name()+");");
				}else if(param.type().is(String.class)){
					$("params = (params==null?\"?\":(params + \"&\")) + \""+param.name()+"=\" + java.net.URLEncoder.encode("+param.name()+", \"UTF-8\");");
				}else if(param.type().isJAnnotationPresent(CrystalDate.class)){
					$("new "+param.type().getSimpleName()+"("+param.name()+");");
					$("params = (params==null?\"?\":(params + \"&\")) + \""+param.name()+"=\" + "+param.name()+";");
				}else 
					throw new NullPointerException("Parametro no reconocido "+param.type().getSimpleName());
				if(param.type().nullable()) {
					$("}");
					METODO.decLevel();
				}
				
			}
			$("if(params != null)ruta+=params;");
		}};
	}
	
	
	public void generateDocumentation(final List<String> opcionesDoc) throws IOException{
		final JavaCode cliente = new JavaCode(){{
				$("package "+paqueteControllers+";");
				$("import java.io.*;");
				$("public class DocumentationGen", ()->{
					$("public static void writeDocumentation(MyReader reader)",()->{
						$("try",()->{
							$("String path = reader.readLine();");
							$("path = path.replaceAll(\"\\\\s+\",\"\");");
							$("path = (!path.isEmpty() && !path.endsWith(\"\\\\\")?path: path+\"\\\\\");");
							$("try(PrintWriter rutas = new PrintWriter(path+\""+rutas+"\"))", ()->{
								$("rutas.println(\"<!DOCTYPE html><html>\");");
								$("rutas.println(\"<head><meta charset='UTF-8'><style>\");");
								$("rutas.println(\"table, h3 {font-family: arial, sans-serif; border-collapse: collapse; width: 100%;}\");");
								$("rutas.println(\"td, th {border: 1px solid #dddddd;text-align: left; padding: 8px;}\");");
								$("rutas.println(\"</style></head>\");");
								$("rutas.println(\"<body>\");");
								for(String op : opcionesDoc)
								$("rutas.println("+op+");");
								$("rutas.println(\"</body></html>\");");
								$("System.out.println(\"Documentación terminada y disponible en:\"+path+ \""+rutas+"\");");
							}, "catch (FileNotFoundException e) {e.printStackTrace();}");
						}, "catch (IOException e) {e.printStackTrace();}");
						
						
					});
				});
		}};
		//        	System.out.println("\t"+param.p.type().getSimpleName() + " " + param.nombre);
		exportFile(cliente, paqueteControllers.replace(".", File.separator) + File.separator + "DocumentationGen.java");
	}
}
