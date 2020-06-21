package jcrystal.clients.webadmin;

import java.util.ArrayList;
import java.util.List;

import jcrystal.configs.clients.admin.AdminClient;
import jcrystal.configs.clients.admin.ListOption;
import jcrystal.configs.clients.admin.SubListOption;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.types.IJType;
import jcrystal.types.JClass;

public class AdminPageDescriptor {
	public final int id;
	private final ClientContext context;
	public final JClass definerClass;
	public final String label, path;
	public final IJType type;
	private final EntityClass entidad;
	
	public JCrystalWebService get;
	public JCrystalWebService delete;
	public AdminWsDescriptor list;
	public AdminWsDescriptor update;
	public AdminWsDescriptor add;
	public List<AdminWsDescriptor> listOptions = new ArrayList<>();
	public List<AdminWsDescriptor> subListOptions = new ArrayList<>();
	
	public AdminPageDescriptor(ClientContext context, JClass definerClass) {
		this.definerClass = definerClass;
		this.context = context;
		id = context.ADMIN_ID_GENERATOR++;
		label = definerClass.getAnnotation(AdminClient.class).label();
		if(definerClass.getAnnotation(AdminClient.class).path().startsWith("/"))
			path = definerClass.getAnnotation(AdminClient.class).path().substring(1);
		else
			path = definerClass.getAnnotation(AdminClient.class).path();
		
		type = context.jClassLoader.forName(definerClass.getAnnotation(AdminClient.class).type());
		this.entidad = context.data.entidades.get(type);
	}
	public void register(JCrystalWebService method) {
		switch (method.name()) {
			case "list":
				if(list==null)
					list = new AdminWsDescriptor();
				list.setWs(method);
			break;
			case "get":
				get = method;
			break;
			case "update":
				if(update==null)
					update = new AdminWsDescriptor();
				update.setWs(method);
			break;
			case "delete":
			delete = method;
			break;
			case "add":
				if(add==null)
					add = new AdminWsDescriptor();
				add.setWs(method);
			break;
			case "login":
				context.data.adminData.login = method;
				
			break;
			case "logout":
				context.data.adminData.logout = method;
			break;
			case "add_source":
				if(add==null)
					add = new AdminWsDescriptor();
				add.setSource(method);
			break;
			case "update_source":
				if(update==null)
					update = new AdminWsDescriptor();
				update.setSource(method);
			break;
			case "list_source":
				if(list==null)list = new AdminWsDescriptor();
				list.setSource(method);
			break;
			
			default:
				if(method.name().endsWith("_source")) {
					AdminWsDescriptor op = listOptions.stream().filter(f->f.ws != null && (f.ws.name()+"_source").equals(method.name())).findFirst().orElse(null);
					if(op == null)listOptions.add(op = new AdminWsDescriptor());
						op.source = method;
				}else if(method.isAnnotationPresent(ListOption.class)) {
					AdminWsDescriptor op = listOptions.stream().filter(f->f.ws != null && f.ws.name().equals(method.name()+"_source")).findFirst().orElse(null);
					if(op == null)listOptions.add(op = new AdminWsDescriptor());
						op.ws = method;
				}else if(method.isAnnotationPresent(SubListOption.class)) {
					AdminWsDescriptor op = subListOptions.stream().filter(f->f.ws != null && f.ws.name().equals(method.name()+"_source")).findFirst().orElse(null);
					if(op == null)
						subListOptions.add(op = new AdminWsDescriptor());
					op.ws = method;
				}
			break;
		}
		
	}
	public EntityClass getEntidad() {
		return entidad;
	}
}
