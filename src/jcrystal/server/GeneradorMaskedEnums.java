package jcrystal.server;

import jcrystal.main.data.ClientContext;
import jcrystal.types.JClass;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorMaskedEnums {
	private ClientContext context;
	
	public GeneradorMaskedEnums(ClientContext context) {
		this.context = context;
	}
	
	public void generar(JClass clase){
		new JavaCode(1) {{
			$("@Override public long id()",()->{
				$("return id;");
			});
			$("private static final "+clase.simpleName+" cachedVals[] = new "+clase.simpleName+"[values().length];");
			$("static",()->{
				$("for("+clase.simpleName+" val : values())cachedVals[Long.numberOfTrailingZeros(val.id)]=val;");
			});
			$("public static "+clase.simpleName+" fromId(long id)",()->{
				$if("id==0","return null;");
				$("return cachedVals[Long.numberOfTrailingZeros(id)];");
			});
			$("public static "+clase.simpleName+" fromId(int id)",()->{
				$if("id==0","return null;");
				$("return cachedVals[Integer.numberOfTrailingZeros(id)];");
			});
			$("public static "+clase.simpleName+"[] getFromMask(long mask)",()->{
				$(clase.simpleName+"[] ret = new " +clase.simpleName+"[Long.bitCount(mask)];");
				$("int pos = 0;");
				$("for("+clase.simpleName+" val : values())",()->{
					$if("(val.id & mask) != 0", "ret[pos++]=val;");
				});
				$("return ret;");
			});
			context.output.addSection(clase, "GEN", this);
		}};
	}
}
