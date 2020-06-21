package jcrystal.main.data;

import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.MainClientGenerator;
import jcrystal.clients.utils.EntityUtils;
import jcrystal.configs.clients.Client;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.GeneradorToJson;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.types.IJType;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.context.ExtendingTypeConverter;
import jcrystal.utils.context.ITypeConverter;
import jcrystal.utils.extractors.GeneratorFromForm;
import jcrystal.utils.extractors.GeneratorFromJsonObject;

public class ClientUtils {

	public final MainClientGenerator clientGenerator; 
	public final ClientGeneratorDescriptor<?> SERVER_DESCRIPTOR;
	private final EntityUtils entityUtils;
	public final GeneradorToJson generadorToJson;
	public final Extractors extrators;
	public final Converters converters;
	private final ClientContext context;
	public ClientUtils(ClientContext context) {
		this.context = context;
		clientGenerator = new MainClientGenerator(context);
		SERVER_DESCRIPTOR = new ClientGeneratorDescriptor<Client>(context, null);
		entityUtils = new EntityUtils(context);
		generadorToJson = new GeneradorToJson(context);
		extrators = new Extractors();
		converters = new Converters();
	}
	public EntityUtils getEntityUtils() {
		return entityUtils;
	}
	public class Extractors{
		public final GeneratorFromForm formData = new GeneratorFromForm();
		public final GeneratorFromJsonObject jsonObject = new GeneratorFromJsonObject(); 
	}
	public class Converters{
		public final ExtendingTypeConverter dates = new ExtendingTypeConverter() {
			@Override
			public IJType extend(IJType type) {
				if (type.isJAnnotationPresent(CrystalDate.class))
					return GlobalTypes.DATE;
				return null;
			}
		};
		public final ExtendingTypeConverter relationsToKeys = new ExtendingTypeConverter() {
			@Override
			public IJType extend(IJType type) {
				if(type.isAnnotationPresent(jEntity.class)) {
					EntityClass entity = context.data.entidades.get(type);
					return entity.key.getSingleKeyType();
				}
				return null;
			}
		};
	}
}
