package jcrystal.reflection;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServlet;

import jcrystal.clients.ClientTagGenerator;
import jcrystal.configs.clients.ResourceType;
import jcrystal.lang.Language;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.ClientInput;
import jcrystal.main.data.ClientOutput;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.IndexableField;
import jcrystal.model.server.db.EntityIndexModel;
import jcrystal.types.JClass;
import jcrystal.server.BackendGenerator;
import jcrystal.server.GeneradorEntidadPush;
import jcrystal.server.GeneradorEnums;
import jcrystal.server.GeneradorJSF;
import jcrystal.server.GenerarQueueCode;
import jcrystal.server.GenerarUtilidades;
import jcrystal.server.databases.ModelGenerator;
import jcrystal.server.web.servlet.GeneradorServlet;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;

public class MainGenerator {
	
	private ClientContext context;
	
	private BackendGenerator backendGenerator;
	
	public MainGenerator(ClientInput input, ClientOutput output) {
		context = new ClientContext(input, output);
		backendGenerator = new BackendGenerator(context);
	}
	
	public static boolean JSON_PATHS = false;
	
	static File getOutputSourceFile(String path, String subpath){
		if(path == null || !new File(path).getParentFile().exists())
			return null;
		if(subpath == null)
			return new File(path);
		return new File(path, subpath);
	}
	
	public ClientContext generar() throws Exception {
		long l = System.currentTimeMillis();
		ContextType.SERVER.init();
		CodeGeneratorContext.set(Language.JAVA, null);
		
		ClientTagGenerator.generate(context);
		context.utils.clientGenerator.loadClients();
		
		context.data.doPreprocess(context);
		
		backendGenerator.generate();
		
		if(context.input.SERVER != null) {
			System.out.println("Generando clientes");
			context.utils.clientGenerator.generarClientes();
		}
		
		System.out.println(System.currentTimeMillis() - l);
		return context;
	}
}
