package jcrystal.preprocess.resolvers;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.types.JIAnnotable;
import jcrystal.types.convertions.IAnnotationResolver;
import jcrystal.configs.clients.admin.*;
import jcrystal.entity.types.security.*;
import jcrystal.json.*;
import jcrystal.reflection.annotations.async.*;
import jcrystal.reflection.annotations.com.*;
import jcrystal.reflection.annotations.*;
import jcrystal.reflection.annotations.entities.*;
import jcrystal.reflection.annotations.security.*;
import jcrystal.reflection.annotations.validation.date.*;
import jcrystal.reflection.annotations.validation.*;
import jcrystal.reflection.annotations.ws.*;
import jcrystal.reflection.docs.*;
import jcrystal.reflection.server.*;
import jcrystal.annotations.server.*;
import jcrystal.server.async.*;
public class AnnotationResolver implements IAnnotationResolver{
	public <A extends Annotation> A resolveAnnotation(Class<A> annotationClass, JIAnnotable element){
		JAnnotation anotacion = element.getAnnotations().get(annotationClass.getName());
		if(anotacion != null){
			switch(annotationClass.getName()){
				case "jcrystal.configs.clients.admin.AdminClient":{
					return (A)new jcrystal.configs.clients.admin.AdminClientWrapper(anotacion);
				}
				case "jcrystal.configs.clients.admin.ListOption":{
					return (A)new jcrystal.configs.clients.admin.ListOptionWrapper(anotacion);
				}
				case "jcrystal.configs.clients.admin.SubListOption":{
					return (A)new jcrystal.configs.clients.admin.SubListOptionWrapper(anotacion);
				}
				case "jcrystal.entity.types.security.GoogleAccountKeys":{
					return (A)new jcrystal.entity.types.security.GoogleAccountKeysWrapper(anotacion);
				}
				case "jcrystal.json.JsonBasic":{
					return (A)new jcrystal.json.JsonBasicWrapper(anotacion);
				}
				case "jcrystal.json.JsonDetail":{
					return (A)new jcrystal.json.JsonDetailWrapper(anotacion);
				}
				case "jcrystal.json.JsonFull":{
					return (A)new jcrystal.json.JsonFullWrapper(anotacion);
				}
				case "jcrystal.json.JsonID":{
					return (A)new jcrystal.json.JsonIDWrapper(anotacion);
				}
				case "jcrystal.json.JsonMin":{
					return (A)new jcrystal.json.JsonMinWrapper(anotacion);
				}
				case "jcrystal.json.JsonNormal":{
					return (A)new jcrystal.json.JsonNormalWrapper(anotacion);
				}
				case "jcrystal.json.JsonString":{
					return (A)new jcrystal.json.JsonStringWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.async.ClientQueueable":{
					return (A)new jcrystal.reflection.annotations.async.ClientQueueableWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.async.Cron":{
					return (A)new jcrystal.reflection.annotations.async.CronWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.async.Queue":{
					return (A)new jcrystal.reflection.annotations.async.QueueWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.async.Queues":{
					return (A)new jcrystal.reflection.annotations.async.QueuesWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.com.jSerializable":{
					return (A)new jcrystal.reflection.annotations.com.jSerializableWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.Def":{
					return (A)new jcrystal.reflection.annotations.DefWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.Delete":{
					return (A)new jcrystal.reflection.annotations.DeleteWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.EntidadPush":{
					return (A)new jcrystal.reflection.annotations.EntidadPushWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.CarbonCopy":{
					return (A)new jcrystal.reflection.annotations.entities.CarbonCopyWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.History":{
					return (A)new jcrystal.reflection.annotations.entities.HistoryWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.OnConstruct":{
					return (A)new jcrystal.reflection.annotations.entities.OnConstructWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.OnCreate":{
					return (A)new jcrystal.reflection.annotations.entities.OnCreateWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.OnDelete":{
					return (A)new jcrystal.reflection.annotations.entities.OnDeleteWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.OnUpdate":{
					return (A)new jcrystal.reflection.annotations.entities.OnUpdateWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.PreWrite":{
					return (A)new jcrystal.reflection.annotations.entities.PreWriteWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.Rel1to1":{
					return (A)new jcrystal.reflection.annotations.entities.Rel1to1Wrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.RelMto1":{
					return (A)new jcrystal.reflection.annotations.entities.RelMto1Wrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.entities.RelMtoM":{
					return (A)new jcrystal.reflection.annotations.entities.RelMtoMWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.EntityIndex":{
					return (A)new jcrystal.reflection.annotations.EntityIndexWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.EntityIndexes":{
					return (A)new jcrystal.reflection.annotations.EntityIndexesWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.EntityKey":{
					return (A)new jcrystal.reflection.annotations.EntityKeyWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.EntityProperty":{
					return (A)new jcrystal.reflection.annotations.EntityPropertyWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.FullJsonImport":{
					return (A)new jcrystal.reflection.annotations.FullJsonImportWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.jEntity":{
					return (A)new jcrystal.reflection.annotations.jEntityWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.LoginResultClass":{
					return (A)new jcrystal.reflection.annotations.LoginResultClassWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.Post":{
					return (A)new jcrystal.reflection.annotations.PostWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.RolEnum":{
					return (A)new jcrystal.reflection.annotations.RolEnumWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.security.HashSalt":{
					return (A)new jcrystal.reflection.annotations.security.HashSaltWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.Selector":{
					return (A)new jcrystal.reflection.annotations.SelectorWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.Transactional":{
					return (A)new jcrystal.reflection.annotations.TransactionalWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.date.GreaterThanValidation":{
					return (A)new jcrystal.reflection.annotations.validation.date.GreaterThanValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.date.LessThanValidation":{
					return (A)new jcrystal.reflection.annotations.validation.date.LessThanValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.EmailValidation":{
					return (A)new jcrystal.reflection.annotations.validation.EmailValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.EmptyValidation":{
					return (A)new jcrystal.reflection.annotations.validation.EmptyValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.MaxValidation":{
					return (A)new jcrystal.reflection.annotations.validation.MaxValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.MinValidation":{
					return (A)new jcrystal.reflection.annotations.validation.MinValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.PasswordValidation":{
					return (A)new jcrystal.reflection.annotations.validation.PasswordValidationWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.validation.Validate":{
					return (A)new jcrystal.reflection.annotations.validation.ValidateWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.ws.DataSource":{
					return (A)new jcrystal.reflection.annotations.ws.DataSourceWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.ws.HeaderParam":{
					return (A)new jcrystal.reflection.annotations.ws.HeaderParamWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.ws.Http":{
					return (A)new jcrystal.reflection.annotations.ws.HttpWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.ws.ResponseHeader":{
					return (A)new jcrystal.reflection.annotations.ws.ResponseHeaderWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.ws.ResponseHeaders":{
					return (A)new jcrystal.reflection.annotations.ws.ResponseHeadersWrapper(anotacion);
				}
				case "jcrystal.reflection.annotations.ws.SingleCallWS":{
					return (A)new jcrystal.reflection.annotations.ws.SingleCallWSWrapper(anotacion);
				}
				case "jcrystal.reflection.docs.Doc":{
					return (A)new jcrystal.reflection.docs.DocWrapper(anotacion);
				}
				case "jcrystal.reflection.docs.Param":{
					return (A)new jcrystal.reflection.docs.ParamWrapper(anotacion);
				}
				case "jcrystal.reflection.docs.Params":{
					return (A)new jcrystal.reflection.docs.ParamsWrapper(anotacion);
				}
				case "jcrystal.reflection.server.OnServerLoad":{
					return (A)new jcrystal.reflection.server.OnServerLoadWrapper(anotacion);
				}
				case "jcrystal.annotations.server.Exposed":{
					return (A)new jcrystal.annotations.server.ExposedWrapper(anotacion);
				}
				case "jcrystal.server.async.Async":{
					return (A)new jcrystal.server.async.AsyncWrapper(anotacion);
				}
			}
		}
		return null;
	}
}
