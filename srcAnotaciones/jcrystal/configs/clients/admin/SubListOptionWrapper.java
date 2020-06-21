package jcrystal.configs.clients.admin;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.configs.clients.admin.SubListOption;
@SuppressWarnings("all")
public class SubListOptionWrapper implements SubListOption{
	private final JAnnotation anotacion;
	public SubListOptionWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String sublistClass(){
		return (String)anotacion.values.get("sublistClass");
	}
	public void sublistClass(String value){
		anotacion.values.put("sublistClass", value);
	}
	public String icon(){
		return (String)anotacion.values.get("icon");
	}
	public void icon(String value){
		anotacion.values.put("icon", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.configs.clients.admin.SubListOption.class;
	}
	@Override public String toString(){
		return "@jcrystal.configs.clients.admin.SubListOption("+"icon="+icon()+", "+"name="+name()+", "+"sublistClass="+sublistClass()+")";
	}
}
