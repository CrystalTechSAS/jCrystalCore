package jcrystal.reflection.annotations.validation;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.validation.EmailValidation;
@SuppressWarnings("all")
public class EmailValidationWrapper implements EmailValidation{
	private final JAnnotation anotacion;
	public EmailValidationWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.validation.EmailValidation.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.validation.EmailValidation("+"value="+value()+")";
	}
}
