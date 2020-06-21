package jcrystal.reflection.annotations.ws;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.ws.ResponseHeader;
@SuppressWarnings("all")
public class ResponseHeaderWrapper implements ResponseHeader{
	private final JAnnotation anotacion;
	public ResponseHeaderWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.ws.ResponseHeader.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.ws.ResponseHeader("+"name="+name()+", "+"value="+value()+")";
	}
}
