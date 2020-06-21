package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonID;
@SuppressWarnings("all")
public class JsonIDWrapper implements JsonID{
	private final JAnnotation anotacion;
	public JsonIDWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonID.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonID("+"value="+java.util.Arrays.toString(value())+")";
	}
}
