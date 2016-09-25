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

SELECT DISTINCT ?name ?position
WHERE {
   ?L a :Location .
   ?L :hasWGS84Geolocation ?geom .
   ?geom geo:geometry ?position .
   ?L rdfs:label ?name .

############# http://linkedgeodata.org/sparql

Prefix lgdo: <http://linkedgeodata.org/ontology/>
Prefix geom: <http://geovocab.org/geometry#>
Prefix ogc: <http://www.opengis.net/ont/geosparql#>
Prefix owl: <http://www.w3.org/2002/07/owl#>
Prefix units: <http://www.opengis.net/ont/sf#>
Prefix geo: <http://www.opengis.net/ont/geosparql#>
Prefix geof: <http://www.opengis.net/def/function/geosparql/>

Select ?l {
  ?s
    rdfs:label ?l ;
    geom:geometry [
      ogc:asWKT ?sg
    ] .
    
    FILTER ( bif:st_intersects (?sg, bif:st_point(74.0059, 40.7127), 100) ) .
}

Select ( ?l AS ?name ) ( ?sg AS ?location )
WHERE 
{
  ?s
    rdfs:label ?l ;
    geom:geometry [
      ogc:asWKT ?sg
    ] .    
  FILTER ( bif:st_intersects (?sg, bif:st_point(74.0059, 40.7127), 10) ) . 
#  FILTER ( ?l = "Border Tibet - Xinjiang"@en ) .
#  FILTER (?l="Border Tibet - Xinjiang"^^xsd:string)
  FILTER regex(?l,'.*tibet.*','i')
}
LIMIT 10
