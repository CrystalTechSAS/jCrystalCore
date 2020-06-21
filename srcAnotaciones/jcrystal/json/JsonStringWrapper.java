package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonString;
@SuppressWarnings("all")
public class JsonStringWrapper implements JsonString{
	private final JAnnotation anotacion;
	public JsonStringWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonString.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonString("+"value="+java.util.Arrays.toString(value())+")";
	}
}
