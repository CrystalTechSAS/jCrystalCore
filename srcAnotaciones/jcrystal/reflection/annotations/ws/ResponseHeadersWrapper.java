package jcrystal.reflection.annotations.ws;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.ws.ResponseHeaders;
@SuppressWarnings("all")
public class ResponseHeadersWrapper implements ResponseHeaders{
	private final JAnnotation anotacion;
	public ResponseHeadersWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public ResponseHeader[] value(){
		return java.util.Arrays.stream((JAnnotation[])anotacion.values.get("value")).map(f->new jcrystal.reflection.annotations.ws.ResponseHeaderWrapper(f)).toArray(f->new jcrystal.reflection.annotations.ws.ResponseHeader[f]);
	}
	public void value(ResponseHeader[] value){
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.ws.ResponseHeaders.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.ws.ResponseHeaders("+"value="+java.util.Arrays.toString(value())+")";
	}
}
