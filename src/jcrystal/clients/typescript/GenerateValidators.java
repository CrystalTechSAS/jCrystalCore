package jcrystal.clients.typescript;

import java.io.File;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jcrystal.clients.AbsEntityValidator;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.validation.EmailValidation;
import jcrystal.reflection.annotations.validation.EmptyValidation;
import jcrystal.reflection.annotations.validation.MaxValidation;
import jcrystal.reflection.annotations.validation.MinValidation;
import jcrystal.reflection.annotations.validation.PasswordValidation;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.TypescriptCode;

public class GenerateValidators extends AbsEntityValidator<WebClientTypescript>{
	public GenerateValidators(WebClientTypescript client) {
		super(client);
	}
	public void create(final EntityClass entidad, TreeSet<JsonLevel> levels){
		if(entidad.properties.stream().filter(f->f.f.isAnyAnnotationPresent(EmailValidation.class, EmptyValidation.class, MaxValidation.class, MinValidation.class, PasswordValidation.class)).count() > 0) {
			new TypescriptCode() {{
				$("import {"+entidad.clase.getSimpleName()+"} from \"./"+entidad.clase.getSimpleName()+"\"");
				levels.stream().map(level->entidad.clase.getSimpleName()+level.baseName()).forEach(className->{
					$("import {"+className+"} from \"./"+className+"\"");
				});
				String interfaces = levels.stream().map(level->entidad.clase.getSimpleName()+level.baseName()).collect(Collectors.joining(" | "));
				$("export class " + entidad.clase.getSimpleName()+"Validator", ()->{
					entidad.properties.stream().forEach(campo->{
						$(campo.fieldName()+"_error : string");
					});
					$("validate(data : "+interfaces+") : boolean",()->{
						$("var valid = true;");
						levels.stream().forEach(level->{
							entidad.properties.stream().forEach(campo->{
								$if("'get"+StringUtils.capitalize(campo.fieldName())+"' in data",()->{
									if(campo.f.isAnyAnnotationPresent(EmailValidation.class, EmptyValidation.class, MaxValidation.class, MinValidation.class, PasswordValidation.class)) {
										$("valid = valid && this.validate"+StringUtils.capitalize(campo.fieldName())+"(data.get"+StringUtils.capitalize(campo.fieldName())+"());");
									}								
								});	
							});
							
						});
						$("return valid;");
					});
					entidad.properties.stream().filter(f->f.f.isAnyAnnotationPresent(EmailValidation.class, EmptyValidation.class, MaxValidation.class, MinValidation.class, PasswordValidation.class)).forEach(campo->{
						if(campo.type().is(String.class))
							$("private validate"+StringUtils.capitalize(campo.fieldName())+"(text : string) : boolean",()->{
								$("this."+campo.fieldName()+"_error = null;");
								campo.f.ifAnnotation(EmptyValidation.class, empty->{
									$if("text == null || text.length == 0",()->{
										$("this."+campo.fieldName()+"_error = \""+empty.value()+"\";");
										$("return false;");
									});
								});
								campo.f.ifAnnotation(MinValidation.class, min->{
									$if("text.length() < "+min.min(),()->{
										$("this."+campo.fieldName()+"_error = \""+min.value()+"\";");
										$("return false;");
									});
								});
								campo.f.ifAnnotation(MaxValidation.class, max->{
									$if("text.length() > "+max.max(),()->{
										$("this."+campo.fieldName()+"_error = \""+max.value()+"\";");
										$("return false;");
									});
								});
								campo.f.ifAnnotation(EmailValidation.class, email->{
									$if("!Patterns.EMAIL_ADDRESS.matcher(text).matches()",()->{
										$("this."+campo.fieldName()+"_error = \""+email.value()+"\";");
										$("return false;");
									});
								});
								PasswordValidation password = campo.f.getAnnotation(PasswordValidation.class);
								$("return true;");
							});
						else
							throw new NullPointerException("Invalid type validation (" + entidad.name() + ") : " + campo.type());
					});
					
				});
				client.exportFile(this, client.paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName() + "Validator.ts");
			}};
		}
	}
}
