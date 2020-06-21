package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonMin;
@SuppressWarnings("all")
public class JsonMinWrapper implements JsonMin{
	private final JAnnotation anotacion;
	public JsonMinWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonMin.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonMin("+"value="+java.util.Arrays.toString(value())+")";
	}
}
