package to.augmented.reality.android.ardb.tests.tiledtest;

import android.widget.ImageView;
import to.augmented.reality.android.ardb.http.maps.MapQuestRequestor;
import to.augmented.reality.android.ardb.http.maps.POI;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Format;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageDensity;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageHeight;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageWidth;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapCircle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapOutputImage;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapType;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapsSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.POIs;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Zoom;
import to.augmented.reality.android.ardb.sourcefacade.maps.MapSource;

import java.util.ArrayList;
import java.util.List;

@MapsSource(name = "MapQuest", key = "VBtbjNHKuiJfvZNcAKbpcZAWAIAslZCX", source = MapSource.MAPQUEST)
@MapCircle(radius = 100)
public class MapQuestQuery
//==========================
{
   public MapQuestQuery(ImageView mapImage) { this.mapImage = mapImage; }

   @ImageWidth int imageWidth = 350;

   @ImageHeight int imageHeight = 350;

   @ImageDensity int density = 2;

   @Zoom int zoom = 17;

   @POIs List<POI> pois = new ArrayList<>();

   @MapType MapQuestRequestor.MapType mapType = MapQuestRequestor.MapType.map;

   @Format MapQuestRequestor.MapImageType imageType = MapQuestRequestor.MapImageType.png;

   @MapOutputImage ImageView mapImage;
}
