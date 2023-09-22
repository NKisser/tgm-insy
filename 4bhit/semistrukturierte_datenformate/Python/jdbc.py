import json

PATH = "./output.json"

with open(PATH, "r") as f:
    data = json.load(f)
    print(json.dumps(data, indent=2))
