prefix : <http://augmented.reality.to/semantic/owl#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX fn: <http://www.w3.org/2005/xpath-functions#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX par: <http://parliament.semwebcentral.org/parliament#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX time: <http://www.w3.org/2006/time#>
PREFIX xml: <http://www.w3.org/XML/1998/namespace>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
prefix geo: <http://www.opengis.net/ont/geosparql#>
prefix geof: <http://www.opengis.net/def/function/geosparql/>
prefix gml: <http://www.opengis.net/ont/gml#>
prefix units: <http://www.opengis.net/ont/sf#>

SELECT DISTINCT ?l ?wkt
WHERE {
   ?L a :Location .
   ?L :hasWGS84Geolocation ?geom .
   ?geom geo:asWKT ?wkt .
   ?L rdfs:label ?l
}
