package jcrystal.annotations.server;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.annotations.server.Exposed;
@SuppressWarnings("all")
public class ExposedWrapper implements Exposed{
	private final JAnnotation anotacion;
	public ExposedWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.annotations.server.Exposed.class;
	}
	@Override public String toString(){
		return "@jcrystal.annotations.server.Exposed("+"value="+java.util.Arrays.toString(value())+")";
	}
}
