package jcrystal.clients.utils;

import jcrystal.json.JsonLevel;
import jcrystal.types.IJType;

public interface ClassJsonLevelProvider {
	JsonLevel getJsonLevel(IJType clase);
}
