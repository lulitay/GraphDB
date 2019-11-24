VERTICES = args[0]
EDGES = args[1]
PROPERTIES = args[2]
graph = JanusGraphFactory.open(PROPERTIES)

// Create graph schema
mgmt = graph.openManagement()
if (mgmt.getPropertyKey('myid') == null) {
    VERSION = graph.makeVertexLabel('version').make();
    AIRPORT = graph.makeVertexLabel('airport').make();
    COUNTRY = graph.makeVertexLabel('country').make();
    CONTINENT = graph.makeVertexLabel('continent').make();

    MYID = mgmt.makePropertyKey('myid').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
    CODE = mgmt.makePropertyKey('code').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    DESC = mgmt.makePropertyKey('desc').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    LONGEST = mgmt.makePropertyKey('longest').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    CITY = mgmt.makePropertyKey('city').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    ELEV = mgmt.makePropertyKey('elev').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    ICAO = mgmt.makePropertyKey('icao').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    RUNWAYS = mgmt.makePropertyKey('runways').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    LAT = mgmt.makePropertyKey('lat').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    LON = mgmt.makePropertyKey('lon').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    DIST = mgmt.makePropertyKey('dist').dataType(String.class).cardinality(Cardinality.SINGLE).make();

    ROUTE = graph.makeEdgeLabel('route').multiplicity(MULTI).make();
    CONTAIN = graph.makeEdgeLabel('contain').multiplicity(SIMPLE).make();

    println 'created schema'
}
mgmt.commit()


// load the data
g = graph.traversal()
batchSize = 10000

// load vertices
new File(VERTICES).eachLine {
    line, linecount ->
    if (line != null && line.trim().length() > 0) {
        field = line.split(";");
        //check = g.V().has('myid', field[0]);
        //if(!check.hasNext() {
        if(field[1] == 'version') {
            v =  graph.addVertex('version');
            v.property('myid', field[0]);
            v.property('code', field[2]);
            v.property('desc', field[5]);
        } else if(field[1] == 'airport') {
            v = graph.addVertex('airport');
            v.property('myid', field[0]);
            v.property('code', field[2]);
            v.property('icao', field[3]);
            v.property('city', field[4]);
            v.property('desc', field[5]);
            v.property('runways', field[6]);
            v.property('longest', field[7]);
            v.property('elev', field[8]);
            v.property('lat', field[9]);
            v.property('lon', field[10]);
        } else if(field[1] == 'country') {
            v = graph.addVertex('country');
            v.property('myid', field[0]);
            v.property('code', field[2]);
            v.property('desc', field[5]);
        } else if(field[1] == 'continent') {
            v = graph.addVertex('continent');
            v.property('myid', field[0]);
            v.property('code', field[2]);
            v.property('desc', field[5]);
        }
        //}

        if (linecount % batchSize == 0) {
            graph.tx().commit()
            println linecount
        }
    }
}

println 'added vertices'

// load edges
new File(EDGES).eachLine {
    line, linecount ->
    if(line != null && line.trim().length() > 0) {
        field = line.split(";");

        if(field[3] == 'route') {
            src = g.V().has('myid', field[0]).next();
            dst = g.V().has('myid', field[1]).next();
            e = src.addEdge('route', dst);
            e.property('myid', field[2]);
            e.property('dist', field[4]);
        } else if(field[3] == 'contain') {
            src = g.V().has('myid', field[0]).next();
            dst = g.V().has('myid', field[1]).next();
            e = src.addEdge('contain', dst);
            e.property('myid', field[2]);
        }

        if(linecount % batchSize == 0) {
            graph.tx().commit()
            println linecount
        }
    }
}

println 'added edges'

// Commit any remaining entries and close the graph
graph.tx().commit();
graph.close();