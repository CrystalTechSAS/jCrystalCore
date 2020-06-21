package jcrystal.server;

import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.validation.Validate;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorJSFValidators {
	private ClientContext context;
	
	public GeneradorJSFValidators(ClientContext context) {
		this.context = context;
	}
	
	public void gen(EntityClass entidad){
		new JavaCode(){{
			$("package jcrystal.gen.entities;");
			$import("java.util.Map", "javax.faces.component.UIComponent","javax.faces.context.FacesContext",
			"javax.faces.validator.FacesValidator","javax.faces.validator.Validator","javax.faces.validator.ValidatorException","org.primefaces.validate.ClientValidator", "javax.faces.application.FacesMessage");
			$("import jcrystal.datetime.*;");
			entidad.iterateKeysAndProperties().filter(f->f.isEnum()).forEach((f)->{
				$("import "+f.type().name()+";");
			});
			$("public class " + entidad.clase.getSimpleName()+"JSFValidators", ()->{
				entidad.iterateKeysAndProperties().forEach(f->{
					Validate val = f.f.getAnnotation(Validate.class);
					$("@FacesValidator(\"" + entidad.clase.getSimpleName() + "." + f.fieldName() + "\")");
					$("public static class " + StringUtils.capitalize(f.fieldName())+"Validator implements Validator, ClientValidator",()->{
						$("@Override public java.util.Map<String, Object> getMetadata()",()->{
							if(f.f.isAnnotationPresent(Validate.class)){
								$("java.util.TreeMap<String, Object> metadata = new java.util.TreeMap<>();");
								if(val.min()!=Integer.MIN_VALUE)
									$("metadata.put(\"data-min\", "+val.min()+");");
								if(val.max()!=Integer.MAX_VALUE)
									$("metadata.put(\"data-max\", "+val.max()+");");
								if(val.notEmpty())
									$("metadata.put(\"data-empty\", true);");
								if(val.trim())
									$("metadata.put(\"data-trim\", true);");
								$("return metadata;");
							}else
								$("return null;");
						});
						$("@Override public String getValidatorId()",()->{
							if(f.f.isAnnotationPresent(Validate.class)){
								if(f.f.type().is(com.google.appengine.api.datastore.ShortBlob.class))
									$("return \"crystalStringCheck\";");
								else
									$("return \"crystal"+f.f.type().getSimpleName()+"Check\";");
							}else
								$("return \"noCheck\";");
						});
						$("@Override public void validate(FacesContext someFacesContext, UIComponent someUIComponent, Object o) throws ValidatorException",()->{
							$("System.out.println(\"Validando "+entidad.clase.getSimpleName() + "." + f.fieldName()+"\");");
							if(f.f.isAnnotationPresent(Validate.class)){
								if(val.notEmpty())
									$if("o == null || " + (val.trim()?"o.toString().trim().isEmpty()":"o.toString().isEmpty()"), ()->{
									$("throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, \"Validation Error\", \""+f.fieldName()+" is required;\"));");
								});
								else $("if(o == null)return;");
								if(f.f.type().is(String.class)){
									if(val.min() != Integer.MIN_VALUE)
										$if("o.toString().length() < " + val.min(),()->{
										$("throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, \"Validation Error\", \""+f.fieldName()+" tiene una longitud inválida;\"));");
									});
									if(val.max() != Integer.MAX_VALUE)
										$if("o.toString().length() > " + val.max(),()->{
										$("throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, \"Validation Error\", \""+f.fieldName()+" tiene una longitud inválida;\"));");
									});
									
								}
							}
						});
					});
				});
			});
			context.output.exportFile(this, "jcrystal/gen/entities/"+entidad.clase.getSimpleName()+"JSFValidators.java");
		}};
	}
}
