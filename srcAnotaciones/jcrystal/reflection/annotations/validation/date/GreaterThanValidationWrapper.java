package jcrystal.reflection.annotations.validation.date;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.validation.date.GreaterThanValidation;
@SuppressWarnings("all")
public class GreaterThanValidationWrapper implements GreaterThanValidation{
	private final JAnnotation anotacion;
	public GreaterThanValidationWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	public String field(){
		return (String)anotacion.values.get("field");
	}
	public void field(String value){
		anotacion.values.put("field", value);
	}
	public boolean orEqual(){
		return Boolean.parseBoolean((String)anotacion.values.get("orEqual"));
	}
	public void orEqual(boolean value){
		anotacion.values.put("orEqual", Boolean.toString(value));
	}
	public boolean now(){
		return Boolean.parseBoolean((String)anotacion.values.get("now"));
	}
	public void now(boolean value){
		anotacion.values.put("now", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.validation.date.GreaterThanValidation.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.validation.date.GreaterThanValidation("+"field="+field()+", "+"now="+now()+", "+"orEqual="+orEqual()+", "+"value="+value()+")";
	}
}
