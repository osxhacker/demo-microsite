Logstash With Loki
==================

The purpose of this document is to capture knowledge gained by incorporating [Logstash](https://www.elastic.co/guide/en/logstash/current/introduction.html) with [Grafana Loki](https://grafana.com/docs/loki/latest/) to perform log aggregation.


## Logstash Loki Client

The [Logstash Loki client](https://grafana.com/docs/loki/latest/send-data/logstash/) is included in the Docker image definition and must be in order to enable aggregation.


## JSON Field Names

[Grafana Loki](https://grafana.com/docs/loki/latest/) does not support ingesting JSON having one or more field names with commonly used separators, such as `.` and `-`.  If **any** field contains one, then the *entire* object will be silently dropped.

As such, the [mutate filter plugin](https://www.elastic.co/guide/en/logstash/current/plugins-filters-mutate.html) is used to rename all [ECS fields](https://www.elastic.co/guide/en/ecs/8.10/ecs-field-reference.html) as well as additional system-specific fields having separators into conformant names.


## Loki Indexing

[How Loki uses labels](https://grafana.com/blog/2020/04/21/how-labels-in-loki-can-make-log-queries-faster-and-easier/#how-loki-uses-labels) is thusly:

> Labels in Loki perform a very important task: They define a stream. More specifically, the combination of every label key and value defines the stream. If just one label value changes, this creates a new stream.

Therefore, the [mutate filter plugin](https://www.elastic.co/guide/en/logstash/current/plugins-filters-mutate.html) again is used to reduce the total number of [ECS fields](https://www.elastic.co/guide/en/ecs/8.10/ecs-field-reference.html) as well as to append the `correlationId` to the `message` field.


# Further Reading

* [Logstash Loki client](https://grafana.com/docs/loki/latest/send-data/logstash/)
* [Loki labels](https://grafana.com/blog/2020/04/21/how-labels-in-loki-can-make-log-queries-faster-and-easier/)


