package jcrystal.reflection.annotations.validation;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.validation.MaxValidation;
@SuppressWarnings("all")
public class MaxValidationWrapper implements MaxValidation{
	private final JAnnotation anotacion;
	public MaxValidationWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	public int max(){
		return Integer.parseInt((String)anotacion.values.get("max"));
	}
	public void max(int value){
		anotacion.values.put("max", Integer.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.validation.MaxValidation.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.validation.MaxValidation("+"max="+max()+", "+"value="+value()+")";
	}
}
