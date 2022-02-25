# Gin

## Using Gin

Method **tab/desc**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  -d '{"id": 1, "jsonrpc": "2.0", "method": "tab/desc", "params": ["processing", "user_agg"]}' \
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
    "DB_NAME": "processing",
    "TBL_NAME": "user_agg",
    "PART_SPEC": [
      {
        "INTEGER_IDX": 0,
        "PKEY_NAME": "dt",
        "PKEY_TYPE": "string"
      }
    ],
    "DB_LOCATION_URI": "hdfs://ensime/warehouse/tablespace/managed/hive/processing.db",
    "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/processing.db/user_agg"
  }
}
```

Method **part/head**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  -d '{"id": 3, "jsonrpc": "2.0", "method": "part/head", "params": [198, 2]}' \
  -H 'content-type: application/json'
```

```json
{
    "id": 3,
    "jsonrpc": "2.0",
    "result": [
        {
            "TBL_ID": 198,
            "SD_ID": 13928,
            "PART_ID": 13717,
            "CREATE_TIME": 1608131750,
            "CD_ID": 198,
            "PART_NAME": "dt=2020-11-22",
            "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/incoming_data.db/mediahills/dt=2020-11-22",
            "PART_SPEC_VAL": [
                {
                    "INTEGER_IDX": 0,
                    "PKEY_NAME": "dt",
                    "PKEY_TYPE": "date",
                    "PART_KEY_VAL": "2020-11-22"
                }
            ]
        },
        {
            "TBL_ID": 198,
            "SD_ID": 40501,
            "PART_ID": 40258,
            "CREATE_TIME": 0,
            "CD_ID": 198,
            "PART_NAME": "dt=2021-02-13",
            "LOCATION": "hdfs://ensime/warehouse/tablespace/external/hive/incoming_data.db/mediahills/dt=2021-02-13",
            "PART_SPEC_VAL": [
                {
                    "INTEGER_IDX": 0,
                    "PKEY_NAME": "dt",
                    "PKEY_TYPE": "date",
                    "PART_KEY_VAL": "2021-02-13"
                }
            ]
        }
    ]
}
```

Method **part/prev**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  -d '{"id": 4, "jsonrpc": "2.0", "method": "part/prev", "params": [59, 1626746198]}' \
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
        "PART_SPEC_VAL": [
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

Method **part/find**:

```shell
curl -s -X POST 'http://127.0.0.1:8080/rpc' \
  -d '{"id": 61966, "jsonrpc": "2.0", "method": "part/find", "params": [248, ["2021", "08", "09", "15"]]}' \
  -H 'content-type: application/json'
```

```json
{
  "id": 61966,
  "jsonrpc": "2.0",
  "result": {
    "TBL_ID": 248,
    "SD_ID": 54309,
    "PART_ID": 54059,
    "CREATE_TIME": 1628514173,
    "CD_ID": 248,
    "PART_NAME": "year=2021/month=08/day=09/hour=15",
    "LOCATION": "hdfs://ensime/data/logs/siren/year=2021/month=08/day=09/hour=15",
    "PART_SPEC_VAL": [
      {
        "INTEGER_IDX": 0,
        "PKEY_NAME": "year",
        "PKEY_TYPE": "string",
        "PART_KEY_VAL": "2021"
      },
      {
        "INTEGER_IDX": 1,
        "PKEY_NAME": "month",
        "PKEY_TYPE": "string",
        "PART_KEY_VAL": "08"
      },
      {
        "INTEGER_IDX": 2,
        "PKEY_NAME": "day",
        "PKEY_TYPE": "string",
        "PART_KEY_VAL": "09"
      },
      {
        "INTEGER_IDX": 3,
        "PKEY_NAME": "hour",
        "PKEY_TYPE": "string",
        "PART_KEY_VAL": "15"
      }
    ]
  }
}
```

Method **part/mark**:

```clojure
(echo
    (rpc :part/mark [#_(TBL_ID) 261
                     #_(PART_KEY_VALS) ["2019-11-22" "moscow" 13]
                     #_(DATA_LOCATION) "/user/pmp/input"]))
```

```clojure
{:id 52445,
 :jsonrpc "2.0",
 :result {:PART_COLS ["dt" "city" "num"],
          :PART_KEY_VALS ["2019-11-22" "moscow" 13],
          :TBL_NAME "test01",
          :CD_ID 261,
          :ADD_PARTITION_SQL ["ALTER TABLE processing.test01 ADD IF NOT EXISTS PARTITION(dt=date '2019-11-22', city='moscow', num=13)"],
          :DATA_LOCATION "/user/pmp/input",
          :DB_NAME "processing",
          :PART_LOCATION "/user/pmp/hive/dt=2019-11-22/city=moscow/num=13",
          :DB_ID 16,
          :TBL_ID 261,
          :LOCATION "hdfs://ensime/user/pmp/hive",
          :SD_ID 64654,
          :PART_TYPES ["date" "string" "int"]}}
```

```text
+-----------------------------------+
|             partition             |
+-----------------------------------+
| dt=2019-11-22/city=moscow/num=13  |
+-----------------------------------+
1 rows selected (0.278 seconds)

+------------+--------------+-------------+--------------+-------------+
| test01.id  | test01.name  |  test01.dt  | test01.city  | test01.num  |
+------------+--------------+-------------+--------------+-------------+
| 1          | "ya.ru"      | 2019-11-22  | moscow       | 13          |
| 2          | "yandex.ru"  | 2019-11-22  | moscow       | 13          |
+------------+--------------+-------------+--------------+-------------+
```

Method **part/drop**:

```clojure
(echo
 (rpc :part/drop [#_(TBL_ID) 261
                  #_(PART_KEY_VALS) ["2019-11-22" "moscow" 13]]))
```

```clojure
{:id 95792,
 :jsonrpc "2.0",
 :result {:TBL_NAME "test01",
          :CD_ID 261,
          :PART_KEY_VAL_STR "2019-11-22moscow13",
          :CREATE_TIME 1645776871,
          :PART_SPEC_VAL [{:INTEGER_IDX 0,
                           :PKEY_NAME "dt",
                           :PKEY_TYPE "date",
                           :PART_KEY_VAL "2019-11-22"}
                          {:INTEGER_IDX 1,
                           :PKEY_NAME "city",
                           :PKEY_TYPE "string",
                           :PART_KEY_VAL "moscow"}
                          {:INTEGER_IDX 2,
                           :PKEY_NAME "num",
                           :PKEY_TYPE "int",
                           :PART_KEY_VAL "13"}],
          :DB_NAME "processing",
          :PART_NAME "dt=2019-11-22/city=moscow/num=13",
          :PART_ID 64453,
          :PART_LOCATION "/user/pmp/hive/dt=2019-11-22/city=moscow/num=13",
          :TBL_ID 261,
          :DROP_PARTITION_SQL ["ALTER TABLE processing.test01 DROP IF EXISTS PARTITION(dt=date '2019-11-22', city='moscow', num=13)"],
          :LOCATION "hdfs://ensime/user/pmp/hive/dt=2019-11-22/city=moscow/num=13",
          :SD_ID 64704}}
```

## Requirements

* Java 8, 11
* Hadoop authentication method is **SIMPLE**
* Supported Hive data types in partitions query is **INT**, **DATE**, **STRING**

Build:
```sh
make jar
```

Environment variables are required:
```text
GIN_HOST
GIN_PORT
HIVE_METASTORE_HOST
HIVE_METASTORE_PORT
HIVE_METASTORE_USERNAME
HIVE_METASTORE_PASSWORD
HIVE_HOST
HIVE_PORT (example: 10000)
HIVE_USERNAME
HIVE_PASSWORD
HADOOP_DEFAULT_FS (example: hdfs://ensime:8020/user/spark)
HADOOP_JOB_UGI (example: spark)
```

Available endpoints:
```text
/rpc
/health
```

## License

Copyright (C) 2022 Vadim Kornev.  
Distributed under the MIT License.
