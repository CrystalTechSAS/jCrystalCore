package jcrystal.clients;

import jcrystal.clients.android.AndroidClient;
import jcrystal.clients.angular.WebClientTypescript;
import jcrystal.clients.console.ConsoleClientGenerator;
import jcrystal.clients.flutter.FlutterClient;
import jcrystal.clients.generics.JavaSEClient;
import jcrystal.clients.ios.SwiftClient;
import jcrystal.clients.js.JQueryClient;
import jcrystal.clients.jsweet.WebClientJSweet;
import jcrystal.clients.webadmin.WebAdminClient;
import jcrystal.configs.clients.Client;
import jcrystal.main.data.ClientContext;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JEnum;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.langAndPlats.TypescriptCode;

import java.util.ArrayList;
import java.util.List;

public class MainClientGenerator {
	
	private ClientContext context;
	public MainClientGenerator(ClientContext context) {
		this.context = context;
	}
	
	private List<AbsClientGenerator> clientes;
	public final List<ClientGeneratorDescriptor> DESCRIPTORS = new ArrayList<>();
	
	public void loadClients(){
		context.input.CLIENT.list.forEach(client->{
			DESCRIPTORS.add(new ClientGeneratorDescriptor<Client>(context, client));
		});
		clientes = new ArrayList<>();
		DESCRIPTORS.forEach(client->{
			switch (client.client.type) {
				case ADMIN:
					clientes.add(new WebAdminClient(client));
					break;
				case ANDROID:
					clientes.add(new AndroidClient(client));
					break;
				case FLUTTER:
					clientes.add(new FlutterClient(client));
					break;
				case IOS:
					clientes.add(new SwiftClient(client));
					break;
				case TYPESCRIPT:
					clientes.add(new WebClientTypescript(client));
					break;
				case CONSOLE:
					clientes.add(new ConsoleClientGenerator(client));
					break;
				case SWEET:
					clientes.add(new WebClientJSweet(client));
					break;
				case JQUERY:
					clientes.add(new JQueryClient(client));
					break;
				case JAVA:
					clientes.add(new JavaSEClient(client));
					break;
				default:
					break;
			}
		});
	}
	public void registrar(JCrystalWebServiceManager controller){
		for(AbsClientGenerator<?> c : clientes) {
			c.registrar(controller, null);
		}
	}
	public void registrar(JCrystalWebServiceManager controller, JCrystalWebService metodo){
		for(AbsClientGenerator<?> c : clientes) {
			c.registrar(controller, metodo);
		}
	}
	
	public void generarClientes()throws Exception{
		prepararEnumeraciones();
		for(AbsClientGenerator c : clientes)
			if(!c.endpoints.isEmpty())
				c.generar();
			else System.out.println("No se puede generar " + c.descriptor.client.type + " " + c.endpoints.size());
	}
	private void prepararEnumeraciones() {
		for(final JClass clase : context.data.BACKENDS.MAIN.enums) {
			JEnum enm = clase.enumData;
			if(!enm.propiedades.containsKey("id")) {
				enm.propiedades.put("id", GlobalTypes.STRING);
				for(EnumValue v : enm.valores)
					v.propiedades.put("id", v.propiedades.get("rawName"));
			}
		}
	}
}
