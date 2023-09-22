from datetime import datetime

from influxdb_client import InfluxDBClient
from influxdb_client import Point
from influxdb_client.client.write_api import SYNCHRONOUS, WriteOptions

URL = 'http://localhost:8086'
TOKEN = '98d6ecce53492f2051067df3ce7198b3fef208500deeabc0f91c6f90fd5f1f80 '
ORG = 'org'
BUCKET = 'insy-4bhit'

QUERY = f'from(bucket: "{BUCKET}") |> range(start: -10m) |> filter(fn: (r) => r._measurement == "temperature")'

client = InfluxDBClient(url=URL, token=TOKEN, org=ORG)
write_api = client.write_api(write_options=WriteOptions(
  batch_size=1000,
  flush_interval=10_000,
  retry_interval=5_000, 
  max_retries=5))
query_api = client.query_api()

response = query_api.query(org = ORG, query = QUERY)

print(QUERY)

for table in response:
    for record in table.records:
        values = record.values
        timestamp = record.get_time().strftime('%H:%M:%S')
        
        print(timestamp, values.get('location'), record.get_field(), record.get_value())