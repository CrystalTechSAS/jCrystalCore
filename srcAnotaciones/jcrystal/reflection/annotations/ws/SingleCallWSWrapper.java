package jcrystal.reflection.annotations.ws;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.ws.SingleCallWS;
@SuppressWarnings("all")
public class SingleCallWSWrapper implements SingleCallWS{
	private final JAnnotation anotacion;
	public SingleCallWSWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public int value(){
		return Integer.parseInt((String)anotacion.values.get("value"));
	}
	public void value(int value){
		anotacion.values.put("value", Integer.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.ws.SingleCallWS.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.ws.SingleCallWS("+"value="+value()+")";
	}
}
