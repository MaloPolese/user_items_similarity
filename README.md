# user_items_similarity

## Usage

### With neo4j database

Run tinkerpop server

```bash
make up-neo4j
```

Populate the database

```bash
make populate-neo4j
```

Open a gremlin console for debugging

```bash
make debug-neo4j
```

### In memory database

Run tinkerpop server

```bash
make up-in-memory
```

Populate the database. This is not necessary, as the in-memory database is populated on start.

```bash
make populate-in-memory
```

Open a gremlin console for debugging

```bash
make debug-in-memory
```

### Clean

```bash
make clean
```
