package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.jEntity;
@SuppressWarnings("all")
public class jEntityWrapper implements jEntity{
	private final JAnnotation anotacion;
	public jEntityWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public boolean internal(){
		return Boolean.parseBoolean((String)anotacion.values.get("internal"));
	}
	public void internal(boolean value){
		anotacion.values.put("internal", Boolean.toString(value));
	}
	public boolean useParentName(){
		return Boolean.parseBoolean((String)anotacion.values.get("useParentName"));
	}
	public void useParentName(boolean value){
		anotacion.values.put("useParentName", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.jEntity.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.jEntity("+"internal="+internal()+", "+"name="+name()+", "+"useParentName="+useParentName()+")";
	}
}
