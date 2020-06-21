package jcrystal.reflection.annotations.ws;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.ws.Http;
import jcrystal.reflection.annotations.ws.Method;
import jcrystal.reflection.annotations.ws.ResponseType;
@SuppressWarnings("all")
public class HttpWrapper implements Http{
	private final JAnnotation anotacion;
	public HttpWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String path(){
		return (String)anotacion.values.get("path");
	}
	public void path(String value){
		anotacion.values.put("path", value);
	}
	public Method method(){
		return Method.valueOf((String)anotacion.values.get("method"));
	}
	public void method(Method value){
		anotacion.values.put("method", value == null ? null : value.name());
	}
	public ContentType[] content(){
		String[] vals = (String[])anotacion.values.get("content");
		ContentType[] ret = new ContentType[vals.length];
		for(int e = 0; e < vals.length; e++){
			ret[e] = ContentType.valueOf(vals[e]);
		}
		return ret;
	}
	public void content(ContentType[] value){
		String[] vals = new String[value.length];
		for(int e = 0; e < value.length; e++){
			vals[e] = value[e].name();
		}
		anotacion.values.put("content", vals);
	}
	public ResponseType response(){
		return ResponseType.valueOf((String)anotacion.values.get("response"));
	}
	public void response(ResponseType value){
		anotacion.values.put("response", value == null ? null : value.name());
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.ws.Http.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.ws.Http("+"content="+java.util.Arrays.toString(content())+", "+"method="+method()+", "+"path="+path()+", "+"response="+response()+")";
	}
}
