package jcrystal.annotations.server;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface Exposed{
	public String[] value() default {};
}
