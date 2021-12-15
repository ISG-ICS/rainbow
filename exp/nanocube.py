import math
import requests
import numpy as np
import random
import time
from config import NanocubeConfig

# global constant
nanocube_url = NanocubeConfig.url
nanocube_dataset = NanocubeConfig.dataset


# zoom is the level of 256x256 tile
def long2tile(lon, zoom):
    return math.floor((lon + 180) / 360 * math.pow(2, zoom))


# zoom is the level of 256x256 tile
def lat2tile(lat, zoom):
    return math.floor(
        (1 - math.log(math.tan(lat * math.pi / 180) + 1 / math.cos(lat * math.pi / 180)) / math.pi) / 2 * math.pow(2,
                                                                                                                   zoom))


# level is the level of global pixel
def pixel2long(x, level):
    return x / math.pow(2, level) * 360 - 180


# level is the level of global pixel
def pixel2lat(y, level):
    n = math.pi - 2 * math.pi * y / math.pow(2, level)
    return 180 / math.pi * math.atan(0.5 * (math.exp(n) - math.exp(-n)))


# pixel2long for webgl-pixel rendering version
def x2lng(x, level):
    return (x+0.5) / math.pow(2, level) * 360 - 180


# pixel2lat for webgl-pixel rendering version
def y2lat(y, level):
    y2 = (180 - (y+0.5) / math.pow(2, level) * 360) * math.pi / 180
    return 360 * math.atan(math.exp(y2)) / math.pi - 90


# level is the level of global pixel
def pixel2longlat(pixel, level):
    # return pixel2long(pixel[0], level), pixel2lat(pixel[1], level)
    return x2lng(pixel[0], level), y2lat(pixel[1], level)


def querytile(x, y, zoom):
    query = "b('location',dive(img2d(" + str(zoom) + "," + str(x) + "," + str(y) + "),8),'img8')"
    return query


def to_matrix(l, n):
    return [l[i: i + n] for i in range(0, len(l), n)]


# query nanocube with given bbox and zoom level
#
# [long0, lat0] - bottom-left corner
# [long1, lat1] - top-right corner
# zoom - zoom of the map
#
# return - a list of coordinates [[long0, lat0], [long1, lat1], ...]
#        - a dict of timings
def query(long0, lat0, long1, lat1, zoom):

    timings = {
        "query": 0.0,
        "unflat": 0.0,
        "coordinate": 0.0,
        "extend": 0.0
    }

    # translate given bbox into a list nanocube queries
    tiles, queries = bbox2queries(long0, lat0, long1, lat1, zoom)

    # for each query, send the query and convert the result pixel indexes into coordinates
    all_coordinates = []
    for q in range(0, len(queries)):

        # print("send queries[" + str(q) + "]: " + queries[q] + " for tile " + str(tiles[q]))
        query = nanocube_url + "q(" + nanocube_dataset + "." + queries[q] + ")"

        print("send queries[" + str(q) + "]: " + query)
        start = time.time()
        result = requests.get(query)
        end = time.time()
        timings["query"] += end - start

        start = time.time()
        data = result.json()
        flat_pixels = data[0]['index_columns'][0]['values']
        # print(flat_pixels)
        pixels = to_matrix(flat_pixels, 2)
        pixels = np.array(pixels)
        end = time.time()
        timings["unflat"] += end - start

        print("result size = " + str(pixels.shape[0]))

        if pixels.shape[0] > 0:
            start = time.time()
            coordinates = pixels2coordinates(pixels, tiles[q], zoom)
            end = time.time()
            timings["coordinate"] += end - start
            start = time.time()
            all_coordinates.extend(coordinates)
            end = time.time()
            timings["extend"] += end - start

    return all_coordinates, timings


# translate a bounding box query into a list of nanocube tile-based queries
#
# [long0, lat0] - left-bottom corner
# [long1, lat1] - right-top corner
# zoom - zoom of the map
#
# nanocube query formats:
# img2d(z, x, y) - query only one quadrant image
#   - z is the level of quadtree, 0 - root, 1 - has 4 images, 2 - has 16 images, ... etc.
#   - x grows left -> right
#   - y grows top -> down
def bbox2queries(long0, lat0, long1, lat1, zoom):
    # bounding box of long and lat
    # e.g.
    # bbox = {
    #     'min': [-138.515625, 21.08450008351735],
    #     'max': [-54.140625, 54.08517342088679]
    # }
    bbox = {
        'min': [long0, lat0],
        'max': [long1, lat1]
    }

    # level of quadrant tiles
    level = zoom

    # mercator projection bounding box of tile indexes x and y
    pbbox = {
        'min': {
            'x': long2tile(bbox['min'][0], level),
            'y': lat2tile(bbox['max'][1], level)
        },
        'max': {
            'x': long2tile(bbox['max'][0], level),
            'y': lat2tile(bbox['min'][1], level)
        }
    }

    tiles = []
    queries = []
    for x in range(math.floor(pbbox['min']['x']), math.floor(pbbox['max']['x'] + 1)):
        for y in range(math.floor(pbbox['min']['y']), math.floor(pbbox['max']['y'] + 1)):
            tiles.append([x, y])
            queries.append(querytile(x, y, level))
    return tiles, queries


# convert a list of pixel local indexes within given tile to a list of coordinates (long, lat)
#
# pixels - numpy array of pixel local indexes within the tile [[x0, y0], [x1, y1], ...]
# tile - the tile to which all pixels belong to
# zoom - zoom of the map
def pixels2coordinates(pixels, tile, zoom):
    # translate the within tile pixel index to global pixel index
    tile_x = tile[0]
    tile_y = tile[1]
    # every pixel_global_x = pixel_x + tile_x * 256
    pixels[:, 0] = pixels[:, 0] + tile_x * 256
    pixels[:, 1] = pixels[:, 1] + tile_y * 256
    # print(pixels)

    # global pixel level
    level = zoom + 8

    coordinates = np.apply_along_axis(pixel2longlat, 1, pixels, level=level)
    # print(coordinates)
    return coordinates.tolist()


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

