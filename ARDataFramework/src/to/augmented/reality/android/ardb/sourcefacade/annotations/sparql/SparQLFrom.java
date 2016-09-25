package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the optional SparQL FROM clauses
 * Example<br>
 * <code>
 *    {@literal @}From({"FROM <http://uri1>", "FROM <http://uri1>"}<br>
 *    or
 *    {@literal @}From({"FROM NAMED <http://uri1>", "FROM NAMED <http://uri1>"}<br>
 *
 *
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLFrom { String[] value(); }