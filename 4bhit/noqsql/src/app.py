from time import sleep
import random

from influxdb_client import InfluxDBClient
from influxdb_client import Point
from influxdb_client.client.write_api import SYNCHRONOUS, WriteOptions

URL = 'http://localhost:8086'
TOKEN = '98d6ecce53492f2051067df3ce7198b3fef208500deeabc0f91c6f90fd5f1f80 '
ORG = 'org'
BUCKET = 'insy-4bhit'

client = InfluxDBClient(url=URL, token=TOKEN, org=ORG)
write_api = client.write_api(write_options=WriteOptions(
  batch_size=1000,
  flush_interval=10_000,
  retry_interval=5_000, 
  max_retries=5))
query_api = client.query_api()
delete_api = client.delete_api()

delete_api.delete(
  start='1970-01-01T00:00:00Z',
  stop='2100-01-01T00:00:00Z',
  predicate='_measurement="temperature"', 
  bucket=BUCKET, org=ORG)

counter = 1

while True:
  temperature = 20 + 2 * random.random()

  point = Point('temperature').tag('location', 'TGM').tag('unit', 'celcius').field('value', temperature)
  write_api.write(bucket=BUCKET, org=ORG, record=point)
  
  print(f"Write point {counter}: {point}")
  sleep(20)

  counter += 1