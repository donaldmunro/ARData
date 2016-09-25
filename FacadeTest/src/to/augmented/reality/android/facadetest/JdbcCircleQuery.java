package to.augmented.reality.android.facadetest;

import to.augmented.reality.android.ardb.jdbc.DatabaseType;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Host;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Password;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Circle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.From;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.JdbcSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.OrderBy;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.SelectColumn;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.Where;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.WhereParameter;

@JdbcSource(name = "source1", type = DatabaseType.POSTGRES, database = "eidb", user = "eidb")
@From(value = "location", alias = "l")
//@Join(joinType = JoinType.INNER, joinCondition="p.productcode = o.productcode")
@OrderBy({"description", "wgs84_location"})
@Circle(column="wgs84_location", radius=10000)
//@BoundingBox(column="wgs84_location", width = 1000, height = 1000)
//@Where("location st_intersects(:location)")
@Where("altitude = :altitude")
public class JdbcCircleQuery implements Cloneable
//=================================================
{
   String source = "SingleCircleQuery";

   @Host String host = "10.0.2.2";

   @Password String password = "eidb";

   @SelectColumn(sequence = 0) String description;

   @SelectColumn(sequence = 1, column = "wgs84_location", alias = "location", spatialToText = true) String location;

   //@Select(sequence = 2) @WhereParameter(name="location", column = "location") String locationParam;

   @SelectColumn(sequence = 2) @WhereParameter(name="altitude", column = "altitude") int altitude = 221;

//   @Override public Object clone() throws CloneNotSupportedException { return super.clone(); }
}
