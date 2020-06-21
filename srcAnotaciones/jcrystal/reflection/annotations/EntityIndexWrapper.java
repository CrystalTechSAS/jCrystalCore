package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.EntityIndex;
@SuppressWarnings("all")
public class EntityIndexWrapper implements EntityIndex{
	private final JAnnotation anotacion;
	public EntityIndexWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.EntityIndex.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.EntityIndex("+"name="+name()+", "+"value="+java.util.Arrays.toString(value())+")";
	}
}
