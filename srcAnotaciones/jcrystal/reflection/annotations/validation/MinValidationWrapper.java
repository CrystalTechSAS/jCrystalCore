package jcrystal.reflection.annotations.validation;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.validation.MinValidation;
@SuppressWarnings("all")
public class MinValidationWrapper implements MinValidation{
	private final JAnnotation anotacion;
	public MinValidationWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	public int min(){
		return Integer.parseInt((String)anotacion.values.get("min"));
	}
	public void min(int value){
		anotacion.values.put("min", Integer.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.validation.MinValidation.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.validation.MinValidation("+"min="+min()+", "+"value="+value()+")";
	}
}
