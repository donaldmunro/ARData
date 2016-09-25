package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;

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
public @interface SparQLSource
//============================
{
   /**
    * Specifies a name for this data source.
    * For example {@literal @}SparQlSource(name="mysource"....
    */
   String name();

   /**
    * Specifies the SparQL dialect.
    * @return
    */
   SparQLDialect dialect() default SparQLDialect.VIRTUOSO;

   /**
    * Specifies the URL of the SparQL server endpoint. Can also be specified using the {@literal @}Url annotation
    * applied to a field or method.<br>
    * Example:<br>
    * <code>
    * {@literal @}SparQlSource(name="name",endpoint="http://linkedgeodata.org/sparql", ....
    * or
    *{@literal @}Url String getEndpoint(); // method can have any name
    * </code>
    * @return
    */
   String endpoint() default "";

   /**
    * Specify the HTTP method to use for requests. Defaults to GET
    */
   HTTP_METHOD method() default HTTP_METHOD.GET;

   /**
    * The user agent to send in the request. Defaults to "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0"
    */
   String agent() default "";

   /**
    * Specifies connection timeout
    * For example {@literal @}SparQlSource(connectionTimeout = 60, connectionTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   long connectionTimeout() default 60;

   /**
    * Specifies database connection timeout unit
    * For example {@literal @}SparQlSource(connectionTimeout = 60, connectionTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   TimeUnit connectionTimeoutUnit() default TimeUnit.SECONDS;

   /**
    * Specifies database read timeout
    * For example {@literal @}SparQlSource(readTimeout = 60, readTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   long readTimeout() default 30;

   /**
    * Specifies database read timeout unit
    * For example {@literal @}SparQlSource(readTimeout = 60, readTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   TimeUnit readTimeoutUnit() default TimeUnit.SECONDS;
}
