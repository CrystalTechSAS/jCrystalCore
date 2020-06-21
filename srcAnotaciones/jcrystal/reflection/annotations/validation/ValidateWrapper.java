package jcrystal.reflection.annotations.validation;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.validation.Validate;
@SuppressWarnings("all")
public class ValidateWrapper implements Validate{
	private final JAnnotation anotacion;
	public ValidateWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public int min(){
		return Integer.parseInt((String)anotacion.values.get("min"));
	}
	public void min(int value){
		anotacion.values.put("min", Integer.toString(value));
	}
	public int max(){
		return Integer.parseInt((String)anotacion.values.get("max"));
	}
	public void max(int value){
		anotacion.values.put("max", Integer.toString(value));
	}
	public boolean trim(){
		return Boolean.parseBoolean((String)anotacion.values.get("trim"));
	}
	public void trim(boolean value){
		anotacion.values.put("trim", Boolean.toString(value));
	}
	public boolean notEmpty(){
		return Boolean.parseBoolean((String)anotacion.values.get("notEmpty"));
	}
	public void notEmpty(boolean value){
		anotacion.values.put("notEmpty", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.validation.Validate.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.validation.Validate("+"max="+max()+", "+"min="+min()+", "+"notEmpty="+notEmpty()+", "+"trim="+trim()+")";
	}
}
