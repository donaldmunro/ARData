package to.augmented.reality.android.facadetest;

import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Circle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Url;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLAddPrefixes;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLLimit;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLOrderBy;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSelect;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSelectVariable;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLWhere;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLWhereParameter;

@SparQLSource(name = "sparql", dialect = SparQLDialect.GEOSPARQL/*, endpoint = "http://127.0.0.1:8080/parliament/sparql"*/)

//@HttpHeaders({@HttpHeader(key = "k1", value = "v1"), @HttpHeader(key = "k2", value = "v2")})
//@SparQLPrefixes({ "PREFIX : <http://augmented.reality.to/semantic/owl#>",
//                  "PREFIX fn: <http://www.w3.org/2005/xpath-functions#>"})
@SparQLAddPrefixes("PREFIX : <http://augmented.reality.to/semantic/owl#>")
@SparQLSelect("SELECT DISTINCT ?name ?position ?altitude")
@SparQLWhere("WHERE\n" +
             "{\n" +
             "   ?L a :Location .\n" +
             "   ?L :hasWGS84Geolocation ?geom .\n" +
             "   ?geom geo:asWKT ?position .\n" +
             "   ?L rdfs:label ?name .\n" +
             "   ?L :locationAltitude ?altitude\n" +
             "   FILTER (?altitude >= ::altitude)" +
             "}")
@SparQLOrderBy("ORDER BY ?name")
@SparQLLimit(50)
@Circle(column = "position", radius = 1000)
public class SparQLCircleQuery implements Cloneable
//==================================================
{
   @Url String url()
   //--------------
   {
      if (MainActivity.isEmulator())
         return "http://10.0.2.2:8080/parliament/sparql";
      else
         return "http://192.168.1.2:8080/parliament/sparql";
   }

   @SparQLSelectVariable String name;

   @SparQLSelectVariable String position;

   @SparQLWhereParameter @SparQLSelectVariable int altitude;
}
