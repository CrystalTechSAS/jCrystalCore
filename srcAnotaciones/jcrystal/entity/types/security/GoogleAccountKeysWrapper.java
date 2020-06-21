package jcrystal.entity.types.security;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.entity.types.security.GoogleAccountKeys;
@SuppressWarnings("all")
public class GoogleAccountKeysWrapper implements GoogleAccountKeys{
	private final JAnnotation anotacion;
	public GoogleAccountKeysWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String tokenId(){
		return (String)anotacion.values.get("tokenId");
	}
	public void tokenId(String value){
		anotacion.values.put("tokenId", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.entity.types.security.GoogleAccountKeys.class;
	}
	@Override public String toString(){
		return "@jcrystal.entity.types.security.GoogleAccountKeys("+"tokenId="+tokenId()+")";
	}
}
