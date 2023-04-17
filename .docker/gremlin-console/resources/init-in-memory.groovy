:remote connect tinkerpop.server conf/remote.yaml session

:remote console

g.V().drop().iterate()
g.E().drop().iterate()

:load /custom-gremlin/resources/main.groovy
MovieLensParser.load(g.graph,'/custom-gremlin/resources/data')


:quit