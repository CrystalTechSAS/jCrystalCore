package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonDetail;
@SuppressWarnings("all")
public class JsonDetailWrapper implements JsonDetail{
	private final JAnnotation anotacion;
	public JsonDetailWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonDetail.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonDetail("+"value="+java.util.Arrays.toString(value())+")";
	}
}
