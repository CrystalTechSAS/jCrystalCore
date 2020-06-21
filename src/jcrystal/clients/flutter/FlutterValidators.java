package jcrystal.clients.flutter;

import java.io.File;
import java.util.TreeSet;

import jcrystal.clients.AbsEntityValidator;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.validation.EmailValidation;
import jcrystal.reflection.annotations.validation.EmptyValidation;
import jcrystal.reflection.annotations.validation.MaxValidation;
import jcrystal.reflection.annotations.validation.MinValidation;
import jcrystal.reflection.annotations.validation.PasswordValidation;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class FlutterValidators extends AbsEntityValidator<FlutterClient>{
	
	public FlutterValidators(FlutterClient client) {
		super(client);
	}
	
	@Override public void create(EntityClass entidad, TreeSet<JsonLevel> levels){
		if(entidad.properties.stream().filter(f->f.f.isAnnotationPresent(EmailValidation.class) || f.f.isAnnotationPresent(EmptyValidation.class) || f.f.isAnnotationPresent(MaxValidation.class) || f.f.isAnnotationPresent(MinValidation.class) || f.f.isAnnotationPresent(PasswordValidation.class)).count() > 0) {
			new JavaCode() {{
				$("package " + FlutterClient.paqueteValidadores + ";");
				$("import android.widget.EditText;");
				$("import android.util.Patterns;");
				$("public class " + entidad.name() + "Validators", () -> {
					entidad.properties.stream().forEach(campo->{
						if(campo.type().is(String.class))
							$("public static boolean validate"+StringUtils.capitalize(campo.fieldName())+"(EditText edit)",()->{
								$("String text = edit.getText().toString();");
								campo.f.ifAnnotation(EmptyValidation.class, empty->{
									$if("text == null || text.isEmpty()",()->{
										$("edit.setError(\""+empty.value()+"\");");
										$("edit.requestFocus();");
										$("return false;");
									});
								});
								campo.f.ifAnnotation(MinValidation.class, min->{
									$if("text.length() < "+min.min(),()->{
										$("edit.setError(\""+min.value()+"\");");
										$("edit.requestFocus();");
										$("return false;");
									});
								});
								campo.f.ifAnnotation(MaxValidation.class, max->{
									$if("text.length() > "+max.max(),()->{
										$("edit.setError(\""+max.value()+"\");");
										$("edit.requestFocus();");
										$("return false;");
									});
								});
								campo.f.ifAnnotation(EmailValidation.class, email->{
									$if("!Patterns.EMAIL_ADDRESS.matcher(text).matches()",()->{
										$("edit.setError(\""+email.value()+"\");");
										$("edit.requestFocus();");
										$("return false;");
									});
								});
								PasswordValidation password = campo.f.getAnnotation(PasswordValidation.class);
								$("return true;");
							});
						else
							throw new NullPointerException("Invalid type validation : " + campo.type());
					});
					
				});
				client.exportFile(this, FlutterClient.paqueteValidadores.replace(".", File.separator) + File.separator + entidad.name() + "Validators.java");
			}};
		}
	}
	
}
