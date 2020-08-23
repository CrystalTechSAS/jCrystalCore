package jcrystal.model.web;

import java.util.ArrayList;
import java.util.List;

import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.webadmin.AbsExtensionCreator;
import jcrystal.configs.clients.admin.AdminClient;
import jcrystal.main.data.ClientContext;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JClass;
import jcrystal.types.JMethod;
import jcrystal.types.JVariable;
import jcrystal.reflection.StoredProcedure;
import jcrystal.reflection.annotations.Delete;
import jcrystal.reflection.server.OnServerLoad;
import jcrystal.server.async.Async;
import jcrystal.utils.PathUtils;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;

public class JCrystalWebServiceManager {
	public final ClientContext context;
	public final JClass clase;
	public final boolean isAdminClient;

	public ArrayList<JCrystalWebService> _metodos = new ArrayList<>();
	public final ArrayList<StoredProcedure> storedProcedures = new ArrayList<>();
	public final List<JVariable> managedAttributes = new ArrayList<>();
	private String rutaClase;
	public final boolean isMultipart;
	public static boolean isManager(IJType clase) {
		return clase.name().contains("controllers.") && !clase.name().contains(".gen.") && clase.getSimpleName().startsWith("Manager");
	}
	public JCrystalWebServiceManager(ClientContext context, JClass jclass) {
		this.context = context;
		this.clase = jclass;
		this.isMultipart = jclass.isInner();
		isAdminClient = clase.isAnnotationPresent(AdminClient.class);
		
		jclass.attributes.stream().filter(f->f.isPublic() && !f.isStatic() && !f.isFinal())
		.filter(f->context.data.isSecurityToken(f.type()))
		.forEach(f->managedAttributes.add(f));
		
		loadMethods(context);
	}
	public String getRutaClase() {
		if(rutaClase != null)
			return rutaClase; 
		String fullName = clase.name;
		fullName = fullName.substring(fullName.indexOf("controllers.")+"controllers.".length());
		if(isMultipart) {
			fullName = fullName.substring(0, fullName.lastIndexOf('$'));
		}
		
		String simpleName = isMultipart ? clase.getDeclaringClass().getSimpleName() : clase.getSimpleName();
		fullName = fullName.replace(simpleName, "");
		String ruta = JCrystalWebService.urlReplacements(fullName).replace(".", "/");
		ruta = ruta.substring(0, ruta.lastIndexOf("/")==-1?0:ruta.lastIndexOf("/"));
		if(simpleName.equals("Manager"))
			return rutaClase = ruta;
		else if(ruta.isEmpty())
			return rutaClase = JCrystalWebService.urlReplacements(StringUtils.lowercalize(simpleName.substring("Manager".length()).trim()));
		else if(simpleName.startsWith("Manager"))
			return rutaClase = ruta + "/"+StringUtils.lowercalize(JCrystalWebService.urlReplacements(simpleName.substring("Manager".length()).trim()));
		else
			return rutaClase = ruta + "/"+StringUtils.lowercalize(JCrystalWebService.urlReplacements(simpleName.replace(".Manager",".").trim()));
	}
	private void loadMethods(ClientContext context){
		/*for(int e = 0; e < getSource().size(); e++){
			if(getSource().get(e).contains("/*") && getSource().get(e).contains("SPs")){
				for(int i = e+1; i < getSource().size() && !getSource().get(i).contains("*TODO/"); i++){
					String encabezado = getSource().get(i);
					List<String> resultados = new ArrayList<>();
					for(;getSource().get(i+1).startsWith("\t");i++){
						resultados.add(getSource().get(i+1).trim());
					}
					storedProcedures.add(new StoredProcedure(encabezado, resultados));
				}
			}
		}*/
		clase.methods.stream().filter(JMethod::isPublic).forEach(m->{
			if(m.isAnnotationPresent(OnServerLoad.class)) {
				if(!m.isStatic() || !m.params.isEmpty())
					throw new NullPointerException("Invalid OnServerLoad Method. " + m.name()+". Should be a static void method with no args.");
				context.data.globalServerLoadMethods.add(m);
			}else if(!m.isAnnotationPresent(Delete.class)){
				JCrystalWebService ws = new JCrystalWebService(context, this, m);
				_metodos.add(ws);
				m.ifAnnotation(Async.class, a->{
					context.data.queues.get(a.name()).tasks.add(ws);
				});
			}
		});
	}
	public void crearSwitchServletManager(final AbsCodeBlock GENERADO){
		GENERADO.new B(){{
				$("case \""+getPath(null)+"\":",()->{
					$("SubServlet"+clase.simpleName+".doMultipart(req, resp);");
					$("break;");
				});
		}};
	}
	public String getSubservletClassName() {
		if(isMultipart)
			return "SubServlet"+clase.declaringClass.getSimpleName().replace("Manager", "");
		return "SubServlet"+clase.simpleName.replace("Manager", "");
	}
	public String getManagerName() {
		if(isMultipart)
			return "Manager" + clase.declaringClass.getSimpleName().replace("Manager", "");
		return "Manager" + clase.simpleName.replace("Manager", "");
	}
	
	public void generateClass(){
		JavaCode extension = AbsExtensionCreator.addExtensions(context, this);
		if(!storedProcedures.isEmpty() || (extension!= null && extension.size() > 2)){
			new JavaCode(){{
				if(!storedProcedures.isEmpty())
					$("class Abs"+clase.simpleName,()->{
						for(StoredProcedure ps : storedProcedures)
							ps.crearMetodo(this);
					});
				if(extension!= null && extension.size() > 2)
					$append(extension);
				context.output.addGlobalSection(clase, "GENERATED", this);
			}};
		}
	}
	
	public String getPath(ClientGeneratorDescriptor<?> clientConfig) {
		String id = exportClientConfigId(clientConfig);
		String path = rutaClase;
		if(isMultipart)
			path += "/"+StringUtils.lowercalize(clase.getSimpleName());
		if("Default".equals(id))
			return PathUtils.root(PathUtils.concat(context.input.SERVER.WEB.servlet_root_path, path)); 
		return PathUtils.root(path);
	}
	public String exportClientConfigId(ClientGeneratorDescriptor<?> clientConfig) {
		if(clientConfig == null || clientConfig.configs.size()<=1)
			return "Default";
		
		JAnnotation b = clase.getJAnnotation(clientConfig.clientAnnotationClass.name());
		if(b == null)
			b = clase.getPackage().getJAnnotation(clientConfig.clientAnnotationClass.name());
		if(b==null)
			return "Default";
		return ClientGeneratorDescriptor.getConfigId(b);
	}
}
