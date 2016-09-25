package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import to.augmented.reality.android.ardb.jdbc.DatabaseType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Defines a class as being JDBC annotated and defines connection parameters for the source.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JdbcSource
//====================================
{
   /**
    * Specifies a name for this data source.
    * For example {@literal @}JdbcSource(name="mysource"....
    */
   String name();

   /**
    * Specifies a database type for this data source (see {@link DatabaseType})
    * For example {@literal @}JdbcSource(type = DatabaseType.POSTGRES...
    */
   DatabaseType type();

   /**
    * Specifies network host address for the database. Can also be specified as an annotated variable or method in
    * the class body. Defaults to localhost if not specified.
    * <code>
    * For example {@literal @}JdbcSource(host = "localhost"  ...
    * or
    * @Host host = "192.168.0.1";
    * </code>
    */
   String host() default "";

   /**
    * Specifies network port for the database. Defaults to the databases default port if not specified.
    * For example {@literal @}JdbcSource(port = 5432  ...
    */
   int port() default -1;

   /**
    * Specifies database name for the database
    * For example {@literal @}JdbcSource(database = "emp"  ...
    */
   String database();

   /**
    * Specifies database user for the database
    * For example {@literal @}JdbcSource(user = "scott"  ...
    */
   String user();

   /**
    * Specifies database password for the database
    * For example {@literal @}JdbcSource(password = "tiger"  ...
    * If not specified then fields or methods annotated with @Password (@link Password} will be used and if none are
    * found then "" will be used.
    */
   String password() default "";

   /**
    * Specifies database connection timeout
    * For example {@literal @}JdbcSource(connectionTimeout = 60, connectionTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   long connectionTimeout() default 60;

   /**
    * Specifies database connection timeout unit
    * For example {@literal @}JdbcSource(connectionTimeout = 60, connectionTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   TimeUnit connectionTimeoutUnit() default TimeUnit.SECONDS;

   /**
    * Specifies database read timeout
    * For example {@literal @}JdbcSource(readTimeout = 60, readTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   long readTimeout() default 30;

   /**
    * Specifies database read timeout unit
    * For example {@literal @}JdbcSource(readTimeout = 60, readTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   TimeUnit readTimeoutUnit() default TimeUnit.SECONDS;
}
