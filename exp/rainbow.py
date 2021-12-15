import websocket
import json
import time
import copy
import struct
from config import RainbowConfig

# global constant
rainbow_url = RainbowConfig.url

# request template
#   by default: algorithm = RAQuadTreeDistance, keyword = %
request_template = {
    "type":"query",
    "keyword":"%",
    "query":{
        "key":"%-RAQuadTreeDistance",
        "zoom":5,
        "bbox":[-138.515625,21.08450008351735,-54.140625,54.08517342088679],
        "algorithm":"RAQuadTreeDistance",
        "resX":1920,
        "resY":978,
        "aggregator":"gl-pixel",
        "sampleSize":"0",
        "keyword":"%"
    }
}

# websocket object
ws = websocket.create_connection(rainbow_url)
print("websocket connection to " + rainbow_url + " established.")


# query rainbow with given request json object
# return - a list of coordinates [[long0, lat0], [long1, lat1], ...]
#        - a dict of timings
def query_json(_request):
    timings = {
        "query": 0.0,
        "decode": 0.0
    }

    print("send query: " + json.dumps(_request))
    start = time.time()
    ws.send(json.dumps(_request))
    
    # wait for progressive computation is done
    progress = 0
    while progress < 100:
        binary = ws.recv_frame()
        start1 = time.time()
        coordinates, progress = decode_binary(binary.data)
        end1 = time.time()
        timings["decode"] += end1 - start1
    
    end = time.time()
    timings["query"] = end - start

    

    return coordinates, timings


# query rainbow with given bbox and zoom
#
# [long0, lat0] - bottom-left corner
# [long1, lat1] - top-right corner
# zoom - zoom of the map
#
# return - a list of coordinates [[long0, lat0], [long1, lat1], ...]
#        - a dict of timings
def query_bbox(long0, lat0, long1, lat1, zoom, budget=0, algorithm="RAQuadTreeDistance", keyword="%"):

    _request = bbox2query(long0, lat0, long1, lat1, zoom, budget, algorithm, keyword)

    return query_json(_request)


def close():
    ws.close()
    print("websocket connection to " + rainbow_url + " closed.")


def bbox2query(long0, lat0, long1, lat1, zoom, budget, algorithm, keyword):
    _request = copy.deepcopy(request_template)
    _request["keyword"] = keyword
    _request["query"]["keyword"] = keyword
    _request["query"]["bbox"] = [long0, lat0, long1, lat1]
    _request["query"]["zoom"] = zoom
    _request["query"]["sampleSize"] = budget
    _request["query"]["algorithm"] = algorithm
    _request["query"]["key"] = keyword + "-" + algorithm
    return _request


#
# decode binary message into a list of coordinates
#
# _data - a binary data with format:
# ---- header ----
#  progress  totalTime  treeTime  aggTime   msgType
# | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 4 BYTES |
# ---- binary data payload ----
#    lat1      lng1      lat2      lng2      ...
# | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | ...
#
# return - a list of coordinates [[long0, lat0], [long1, lat1], ...], progress
def decode_binary(_data):
    bytes = bytearray(_data)

    # header
    progress = int.from_bytes(bytes[0:4], byteorder='big')
    # print("progress = " + str(progress))
    total_time = struct.unpack('!d', bytes[4:12])[0]
    # print("total_time = " + str(total_time))
    tree_time = struct.unpack('!d', bytes[12:20])[0]
    # print("tree_time = " + str(tree_time))
    agg_time = struct.unpack('!d', bytes[20:28])[0]
    # print("agg_time = " + str(agg_time))
    msg_type = int.from_bytes(bytes[28:32], byteorder='big')
    # print("msg_type = " + str(msg_type))

    # payload
    length = int((len(bytes) - 32) / 16)
    coordinates = []
    for i in range(0, length):
        offset = 32 + i * 16
        lat = struct.unpack('!d', bytes[offset: offset + 8])[0]
        long = struct.unpack('!d', bytes[offset + 8: offset + 16])[0]
        coordinates.append([long, lat])

    return coordinates, progress


# write coordinates list to file
# coordinates - list of coordinates [[long0, lat0], [long1, lat1], ...]]
# file - file name
# latlong - write out in order as lat and long, default is True
def write2file(coordinates, file, latlong=True):
    f = open(file, "w")
    if (latlong):
        for coordinate in coordinates:
            coordinate[0], coordinate[1] = coordinate[1], coordinate[0]
    f.write(str(coordinates))
    f.close()


