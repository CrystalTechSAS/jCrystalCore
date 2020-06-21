package jcrystal.server;

import javax.servlet.http.HttpServlet;

import jcrystal.lang.Language;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.SerializableClass;
import jcrystal.reflection.RolGenerator;
import jcrystal.server.databases.ModelGenerator;
import jcrystal.server.web.servlet.GeneradorServlet;
import jcrystal.types.JClass;
import jcrystal.utils.context.CodeGeneratorContext;

public class BackendGenerator {

	private ClientContext context;

	private ModelGenerator generadorEntidades;
	private GeneradorEntidadPush generadorEntidadPush;
	private GeneradorEnums generadorEnums;
	private GeneradorJSF generadorJSF;
	private GenerarQueueCode generarQueueCode;
	private GenerarUtilidades generarUtilidades;
	private GeneradorServlet generadorServlet;
	
	public BackendGenerator(ClientContext context) {
		super();
		this.context = context;
		generadorEntidades = new ModelGenerator(context);
		generadorEntidadPush = new GeneradorEntidadPush(context);
		generadorJSF = new GeneradorJSF(context);
		generarQueueCode = new GenerarQueueCode(context);
		generarUtilidades = new GenerarUtilidades(context);
		generadorServlet = new GeneradorServlet(context);
		generadorEnums = new GeneradorEnums(context);
	}
	
	public void generate() throws Exception{
		CodeGeneratorContext.set(Language.JAVA, new ServerTypeFormatter(context));
		
		if(context.data.rolClass != null)
			new RolGenerator(context, context.data.rolClass).generar();
		
		if(context.input.SERVER != null) {
			if(!context.data.clases_controladores.isEmpty() && context.input.SERVER.WEB != null) {
				generadorServlet.generarServlets();
				generadorJSF.gen();
				if(context.input.CHECKED_CLASSES.contains(HttpServlet.class.getName())) {
					generarQueueCode.generar();
				}
				generarUtilidades.generar();
			}
			generadorEntidades.generarEntidades(context.data.BACKENDS.MAIN);
			
			for (SerializableClass jsonClass : context.data.serializableClasses.values())
				context.utils.generadorToJson.jsonificarClase(jsonClass);
			for (JClass jsonClass : context.data.clases_push_entities)
				generadorEntidadPush.generarEntidad(jsonClass);
			
			generadorEnums.generate(context.data.BACKENDS.MAIN);
		}
		for(BackendWrapper back : context.data.BACKENDS.LIST) {
			generadorEntidades.generarEntidades(back);
			generadorEnums.generate(back);
		}
	}
}
