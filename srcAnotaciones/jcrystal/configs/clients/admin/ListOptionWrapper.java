package jcrystal.configs.clients.admin;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.configs.clients.admin.ListOption;
@SuppressWarnings("all")
public class ListOptionWrapper implements ListOption{
	private final JAnnotation anotacion;
	public ListOptionWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String icon(){
		return (String)anotacion.values.get("icon");
	}
	public void icon(String value){
		anotacion.values.put("icon", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.configs.clients.admin.ListOption.class;
	}
	@Override public String toString(){
		return "@jcrystal.configs.clients.admin.ListOption("+"icon="+icon()+", "+"name="+name()+")";
	}
}
