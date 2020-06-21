package jcrystal.server.databases.google.datastore;

import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.server.databases.AbsContextGenerator;
import jcrystal.utils.StringUtils;

public class ContextGenerator extends AbsContextGenerator {

	public ContextGenerator(ClientContext context) {
		super(DBType.GOOGLE_DATASTORE, context);
	}

	@Override
	protected void generateContent() {
		$("public final com.google.cloud.bigquery.BigQuery service = com.google.cloud.bigquery.BigQueryOptions.getDefaultInstance().getService();");
		$("public final String datasetId;");
		$("protected " + StringUtils.camelizar(type.name())+"(String datasetId)",()->{
			$("this.datasetId = datasetId;");
		});
		
		$("public final com.google.cloud.bigquery.InsertAllRequest.Builder createBuilder(String tableName)",()->{
			$("return com.google.cloud.bigquery.InsertAllRequest.newBuilder(datasetId, tableName);");
		});
		$("public void createDataset()",()->{
			$("service.create(com.google.cloud.bigquery.DatasetInfo.newBuilder(datasetId).build());");
		});
		$("public void createTable(String tableName, com.google.cloud.bigquery.TableDefinition definition)",()->{
			$("service.create(com.google.cloud.bigquery.TableInfo.of(com.google.cloud.bigquery.TableId.of(datasetId, tableName), definition));");
		});
	}

}
