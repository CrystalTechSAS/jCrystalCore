package jcrystal.clients.ios;

import java.io.File;
import java.util.TreeSet;

import jcrystal.types.JClass;
import jcrystal.types.JVariable;
import jcrystal.clients.AbsEntityValidator;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.validation.EmailValidation;
import jcrystal.reflection.annotations.validation.EmptyValidation;
import jcrystal.reflection.annotations.validation.MaxValidation;
import jcrystal.reflection.annotations.validation.MinValidation;
import jcrystal.reflection.annotations.validation.PasswordValidation;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.SwiftCode;

public class IosValidators extends AbsEntityValidator<SwiftClient>{

	public IosValidators(SwiftClient client) {
		super(client);
	}
	@Override
	public void validateResult(JClass clase) {
		if(clase.attributes.stream().anyMatch(IosValidators::hasValidation))
			new SwiftCode() {{
				$("import UIKit");
				$("import jCrystaliOSPackage");
				$("class " + clase.getSimpleName()+"Validators", () -> {
					clase.attributes.stream().filter(IosValidators::hasValidation).forEach(campo->{
						doValidation(this, campo.name(), campo);
					});
					
				});
				client.exportFile(this, SwiftClient.paqueteValidadores.replace(".", File.separator) + File.separator + clase.getSimpleName() + "Validators.swift");
			}};
	}
	@Override
	public void create(EntityClass entidad, TreeSet<JsonLevel> levels) {
		if(entidad.properties.stream().filter(f->hasValidation(f.f)).count() > 0) {
			new SwiftCode() {{
				$("import UIKit");
				$("import jCrystaliOSPackage");
				$("class " + entidad.name()+"Validators", () -> {
					entidad.properties.stream().filter(f->hasValidation(f.f)).forEach(campo->{
						doValidation(this, campo.fieldName(), campo.f);
					});
					
				});
				client.exportFile(this, SwiftClient.paqueteValidadores.replace(".", File.separator) + File.separator + entidad.name() + "Validators.swift");
			}};
			
		}
	}
	private static void doValidation(SwiftCode code, String name, JVariable campo) {
		code.new B() {{
			$("static func validate"+StringUtils.capitalize(StringUtils.camelizar(name))+"(edit : ValidationProtocol, error : (String)->())->Bool",()->{
				EmptyValidation empty = campo.getAnnotation(EmptyValidation.class);
				$if("let text = edit.inputText",()->{
					if(empty != null)
						$if("text.isEmpty",()->{
							$("error(\""+empty.value()+"\")");
							$("return false");
						});
					MinValidation min = campo.getAnnotation(MinValidation.class);
					if(min != null)
						$if("text.count < "+min.min(),()->{
							$("error(\""+min.value()+"\")");
							$("return false");
						});
					MaxValidation max = campo.getAnnotation(MaxValidation.class);
					if(max != null)
						$if("text.count > "+max.max(),()->{
							$("error(\""+max.value()+"\")");
							$("return false");
						});
					EmailValidation email = campo.getAnnotation(EmailValidation.class);
					if(email != null)
						$if("!text.isValidEmail()",()->{
							$("error(\""+email.value()+"\")");
							$("return false");
						});
					PasswordValidation password = campo.getAnnotation(PasswordValidation.class);
					$("return true");
				});
				if(empty != null) {
					$("error(\""+empty.value()+"\")");
					$("return false");
				}else
					$("return true");
			});
		}};
	}
	private static boolean hasValidation(JVariable f) {
		return f.isAnnotationPresent(EmailValidation.class) || f.isAnnotationPresent(EmptyValidation.class) || f.isAnnotationPresent(MaxValidation.class) || f.isAnnotationPresent(MinValidation.class) || f.isAnnotationPresent(PasswordValidation.class);
	}
}
