package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.CarbonCopy;
@SuppressWarnings("all")
public class CarbonCopyWrapper implements CarbonCopy{
	private final JAnnotation anotacion;
	public CarbonCopyWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.CarbonCopy.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.CarbonCopy("+"value="+value()+")";
	}
}
