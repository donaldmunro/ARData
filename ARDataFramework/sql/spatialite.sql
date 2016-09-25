select  AsText(geom),
        MbrWithin(Transform(geom, 25832),
                     BuildCircleMbr(X(Transform(MakePoint(25.99, -34.0001, 4326), 25832)),
                     Y(Transform(MakePoint(25.99, -34.0001, 4326), 25832)),
                     1000, 25832))
from test_pt;
