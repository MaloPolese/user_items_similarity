
INTERESTED_IN

```cypher
MATCH (user:User)-[:WATCHED|RATED]->(m:Movie)-[:ACTS_IN|WROTE|DIRECTED|PRODUCED|HAS]-(feature)
WITH user, feature, count(feature) as supp
WHERE supp > 2
MERGE (user)-[r:INTERESTED_IN]->(feature) 
```
