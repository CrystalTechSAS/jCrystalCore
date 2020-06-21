package jcrystal.configs.clients.admin;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.configs.clients.admin.AdminClient;
@SuppressWarnings("all")
public class AdminClientWrapper implements AdminClient{
	private final JAnnotation anotacion;
	public AdminClientWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String type(){
		return (String)anotacion.values.get("type");
	}
	public void type(String value){
		anotacion.values.put("type", value);
	}
	public String path(){
		return (String)anotacion.values.get("path");
	}
	public void path(String value){
		anotacion.values.put("path", value);
	}
	public String label(){
		return (String)anotacion.values.get("label");
	}
	public void label(String value){
		anotacion.values.put("label", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.configs.clients.admin.AdminClient.class;
	}
	@Override public String toString(){
		return "@jcrystal.configs.clients.admin.AdminClient("+"label="+label()+", "+"path="+path()+", "+"type="+type()+")";
	}
}
