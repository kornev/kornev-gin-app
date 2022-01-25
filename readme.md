# Gin

## Using Gin

Method **table/describe**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  --data '{"id": 1, "jsonrpc": "2.0", "method": "table/describe", "params": ["processing", "user_agg"]}' \
  -H 'content-type: application/json'
```

```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "DB_ID": 16,
    "TBL_ID": 59,
    "SD_ID": 59,
    "CD_ID": 59,
    "DB_LOCATION_URI": "hdfs://ensime/warehouse/tablespace/managed/hive/processing.db",
    "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/processing.db/user_agg"
  }
}
```

Method **partition/list**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  --data '{"id": 3, "jsonrpc": "2.0", "method": "partition/list", "params": [59, 2]}' \
  -H 'content-type: application/json'
```

```json
{
  "id": 3,
  "jsonrpc": "2.0",
  "result": [
    {
      "TBL_ID": 59,
      "SD_ID": 52809,
      "PART_ID": 52559,
      "CREATE_TIME": 1626746198,
      "CD_ID": 59,
      "PART_NAME": "dt=2021-07-19",
      "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/processing.db/user_agg/dt=2021-07-19",
      "PART_SCHEMA": [
        {
          "INTEGER_IDX": 0,
          "PKEY_NAME": "dt",
          "PKEY_TYPE": "string",
          "PART_KEY_VAL": "2021-07-19"
        }
      ]
    },
    {
      "TBL_ID": 59,
      "SD_ID": 52731,
      "PART_ID": 52481,
      "CREATE_TIME": 1626659658,
      "CD_ID": 59,
      "PART_NAME": "dt=2021-07-18",
      "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/processing.db/user_agg/dt=2021-07-18",
      "PART_SCHEMA": [
        {
          "INTEGER_IDX": 0,
          "PKEY_NAME": "dt",
          "PKEY_TYPE": "string",
          "PART_KEY_VAL": "2021-07-18"
        }
      ]
    }
  ]
}
```

Method **partition/previous**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  --data '{"id": 4, "jsonrpc": "2.0", "method": "partition/previous", "params": [59, 1626746198]}' \
  -H 'content-type: application/json'
```

```json
{
  "id": 4,
  "jsonrpc": "2.0",
  "result": {
    "TBL_ID": 59,
    "SD_ID": 52731,
    "PART_ID": 52481,
    "CREATE_TIME": 1626659658,
    "CD_ID": 59,
    "PART_NAME": "dt=2021-07-18",
    "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/processing.db/user_agg/dt=2021-07-18",
    "PART_SCHEMA": [
      {
        "INTEGER_IDX": 0,
        "PKEY_NAME": "dt",
        "PKEY_TYPE": "string",
        "PART_KEY_VAL": "2021-07-18"
      }
    ]
  }
}
```

## Requirements

Build:
```sh
make jar
```

Environment variables are required:
```text
GIN_HOST=?
GIN_PORT=?
HIVE_METASTORE_HOST=?
HIVE_METASTORE_PORT=?
HIVE_METASTORE_USERNAME=?
HIVE_METASTORE_PASSWORD=?
```

Available endpoints:
```text
/rpc
/health
```

## License

Copyright (C) 2022 Vadim Kornev.  
Distributed under the MIT License.
