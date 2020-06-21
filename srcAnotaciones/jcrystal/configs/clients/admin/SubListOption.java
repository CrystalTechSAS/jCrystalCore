package jcrystal.configs.clients.admin;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface SubListOption{
	String name();
	String icon();
	String sublistClass();
}
