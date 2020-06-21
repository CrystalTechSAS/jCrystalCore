package jcrystal.reflection.annotations.ws;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.ws.HeaderParam;
@SuppressWarnings("all")
public class HeaderParamWrapper implements HeaderParam{
	private final JAnnotation anotacion;
	public HeaderParamWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.ws.HeaderParam.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.ws.HeaderParam()";
	}
}
