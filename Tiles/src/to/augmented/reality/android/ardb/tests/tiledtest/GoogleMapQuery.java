package to.augmented.reality.android.ardb.tests.tiledtest;

import android.widget.ImageView;
import to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor;
import to.augmented.reality.android.ardb.http.maps.POI;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Format;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageDensity;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageHeight;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageWidth;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapOutputImage;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapType;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapsSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.POIs;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Zoom;
import to.augmented.reality.android.ardb.sourcefacade.maps.MapSource;

import java.util.ArrayList;
import java.util.List;

@MapsSource(name = "GoogleMaps", key = "AIzaSyDBocUN5N4JcQSCJpkXCfNt38IzPDaTrr4", source = MapSource.GOOGLE)
public class GoogleMapQuery
//==========================
{
   public GoogleMapQuery(ImageView mapImage) { this.mapImage = mapImage; }

   @ImageWidth int imageWidth = 350;

   @ImageHeight int imageHeight = 350;

   @ImageDensity int density = 2;

   @Zoom int zoom = 17;

   @POIs List<POI> pois = new ArrayList<>();

   @MapType GoogleMapRequestor.MapType mapType = GoogleMapRequestor.MapType.roadmap;

   @Format GoogleMapRequestor.MapImageType imageType = GoogleMapRequestor.MapImageType.png;

   @MapOutputImage ImageView mapImage;
}
