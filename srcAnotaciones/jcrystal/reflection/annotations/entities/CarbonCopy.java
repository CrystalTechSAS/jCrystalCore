package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface CarbonCopy{
	String value();
}
