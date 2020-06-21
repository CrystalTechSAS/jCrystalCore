package jcrystal.reflection.annotations.async;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.async.Cron;
@SuppressWarnings("all")
public class CronWrapper implements Cron{
	private final JAnnotation anotacion;
	public CronWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	public int minRetrySec(){
		return Integer.parseInt((String)anotacion.values.get("minRetrySec"));
	}
	public void minRetrySec(int value){
		anotacion.values.put("minRetrySec", Integer.toString(value));
	}
	public int maxDoublings(){
		return Integer.parseInt((String)anotacion.values.get("maxDoublings"));
	}
	public void maxDoublings(int value){
		anotacion.values.put("maxDoublings", Integer.toString(value));
	}
	public String timeZone(){
		return (String)anotacion.values.get("timeZone");
	}
	public void timeZone(String value){
		anotacion.values.put("timeZone", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.async.Cron.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.async.Cron("+"maxDoublings="+maxDoublings()+", "+"minRetrySec="+minRetrySec()+", "+"timeZone="+timeZone()+", "+"value="+value()+")";
	}
}
