VERTICES = args[0]
EDGES = args[1]
PROPERTIES = args[2]
graph = JanusGraphFactory.open(PROPERTIES)

// Create graph schema
 mgmt = graph.openManagement()
if (mgmt.getPropertyKey('myid') == null) {
    VERSION = mgmt.makeVertexLabel('version').make();
    AIRPORT = mgmt.makeVertexLabel('airport').make();
    COUNTRY = mgmt.makeVertexLabel('country').make();
    CONTINENT = mgmt.makeVertexLabel('continent').make();

    MYID = mgmt.makePropertyKey('myid').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
    CODE = mgmt.makePropertyKey('code').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    DESC = mgmt.makePropertyKey('desc').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    LONGEST = mgmt.makePropertyKey('longest').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
    CITY = mgmt.makePropertyKey('city').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    ELEV = mgmt.makePropertyKey('elev').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
    ICAO = mgmt.makePropertyKey('icao').dataType(String.class).cardinality(Cardinality.SINGLE).make();
    RUNWAYS = mgmt.makePropertyKey('runways').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
    LAT = mgmt.makePropertyKey('lat').dataType(Double.class).cardinality(Cardinality.SINGLE).make();
    LON = mgmt.makePropertyKey('lon').dataType(Double.class).cardinality(Cardinality.SINGLE).make();
    DIST = mgmt.makePropertyKey('dist').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();

    ROUTE = mgmt.makeEdgeLabel('route').multiplicity(MULTI).make();
    CONTAIN = mgmt.makeEdgeLabel('contain').multiplicity(SIMPLE).make();

    println 'created schema'
}
mgmt.commit()

println 'creating index'
graph.tx().rollback()
 mgmt = graph.openManagement()
 mgmt.buildIndex('myidIndex', Vertex.class).addKey(mgmt.getPropertyKey('myid')).buildCompositeIndex()
 mgmt.commit()

 //Reindex the existing data
 ManagementSystem.awaitGraphIndexStatus(graph, 'myidIndex').call()
 mgmt = graph.openManagement()
 mgmt.updateIndex(mgmt.getGraphIndex("myidIndex"), SchemaAction.REINDEX).get()
 mgmt.commit()

graph.tx().rollback()
 mgmt = graph.openManagement()
 mgmt.buildIndex('myidIndexEdge', Edge.class).addKey(mgmt.getPropertyKey('myid')).buildCompositeIndex()
 mgmt.commit()

 ManagementSystem.awaitGraphIndexStatus(graph, 'myidIndexEdge').call()
 mgmt = graph.openManagement()
 mgmt.updateIndex(mgmt.getGraphIndex("myidIndexEdge"), SchemaAction.REINDEX).get()
 mgmt.commit()

println 'starting import'

// load the data
g = graph.traversal()
batchSize = 1000

// load vertices
new File(VERTICES).eachLine {
    line, linecount ->
    if (line != null && line.trim().length() > 0) {
        field = line.split(";");

        if(field[1] == 'version') {
            check = g.V().has('myid', field[0]);
            if(!check.hasNext()) {
                v =  graph.addVertex('version');
                v.property('myid', field[0]);
                v.property('code', field[2]);
                v.property('desc', field[5]);
            }
        } else if(field[1] == 'airport') {
            check = g.V().has('myid', field[0]);
            if(!check.hasNext()) {
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
            }
        } else if(field[1] == 'country') {
            check = g.V().has('myid', field[0]);
            if(!check.hasNext()) {
                v = graph.addVertex('country');
                v.property('myid', field[0]);
                v.property('code', field[2]);
                v.property('desc', field[5]);
            }
        } else if(field[1] == 'continent') {
            check = g.V().has('myid', field[0]);
            if(!check.hasNext()) {
                v = graph.addVertex('continent');
                v.property('myid', field[0]);
                v.property('code', field[2]);
                v.property('desc', field[5]);
            }
        }

        if (linecount % batchSize == 0) {
            graph.tx().commit()
            println linecount
        }
    }
}

graph.tx().commit()
println 'added vertices'

// load edges
new File(EDGES).eachLine {
    line, linecount ->
    if(line != null && line.trim().length() > 0) {
        field = line.split(";");

        if(field[3] == 'route') {
            check = g.E().has('myid', field[2]);
            if(!check.hasNext()) {
                src = g.V().has('myid', field[0]).next();
                dst = g.V().has('myid', field[1]).next();
                e = src.addEdge('route', dst);
                e.property('myid', field[2]);
                e.property('dist', field[4]);
            }
        } else if(field[3] == 'contains') {
            check = g.E().has('myid', field[2]);
            if(!check.hasNext()) {
                src = g.V().has('myid', field[0]).next();
                dst = g.V().has('myid', field[1]).next();
                e = src.addEdge('contain', dst);
                e.property('myid', field[2]);
            }
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
