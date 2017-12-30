package avve.textpreprocess.hyperonym;

public enum HyperonymPropertyName
{
	DB_HOST("db.host"), DB_USER("db.user"), DB_PASSWORD("db.password");
	
	final String propertyKey;
	
	HyperonymPropertyName(final String propertyKey)
	{
		this.propertyKey = propertyKey;
	}
}