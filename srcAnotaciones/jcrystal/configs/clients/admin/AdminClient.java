package jcrystal.configs.clients.admin;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminClient{
	String type();
	String path();
	String label();
}
