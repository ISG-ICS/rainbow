import rainbow
import time
import csv
from image_util import compare_points_sets


###############################################################
#  warm up _algorithm
###############################################################
def warm_up(_algorithm, _keyword):
    print("warm up rainbow server for algorithm: " + _algorithm + ", keyword: " + _keyword)
    # warm up query, by default for RAQuadTreeDistance algorithm
    warmup_query = {
        "type":"query",
        "keyword":_keyword,
        "query":{
            "key":_keyword + "-" + _algorithm,
            "zoom":5,
            "bbox":[-138.515625,21.08450008351735,-54.140625,54.08517342088679],
            "algorithm":_algorithm,
            "resX":1920,
            "resY":978,
            "aggregator":"gl-pixel",
            "sampleSize":"5000",
            "keyword":_keyword
        }
    }
    start = time.time()
    coordinates, timings = rainbow.query_json(warmup_query)
    end = time.time()
    print("warm up done.")
    print("warm up time: " + str(end - start) + " seconds.")


###############################################################
#  query _algorithm for ground-truth
###############################################################
def query_ground_truth(_data_set, _long0, _lat0, _long1, _lat1, _zoom, _algorithm, _keyword):
    start = time.time()
    coordinates, timings = rainbow.query_bbox(_long0, _lat0, _long1, _lat1, _zoom, budget=1000000, algorithm=_algorithm, keyword=_keyword)
    end = time.time()
    print("final result size: " + str(len(coordinates)))
    print("total time: " + str(end - start) + " seconds.")
    print("    - query time: " + str(timings["query"]) + " seconds.")
    print("    - decode time: " + str(timings["decode"]) + " seconds.")
    file = _data_set + "_" + _keyword + "_" + _algorithm + "_all.txt"
    rainbow.write2file(coordinates, file)


###############################################################
#  query _algorithm with different _budgets
#   - 20K, 40K, 60K, 80K, 100K
###############################################################
def query_different_budgets(_data_set, _long0, _lat0, _long1, _lat1, _zoom, _budgets, _algorithm, _keyword):
    # budget, query time, decode time
    times = {}
    for budget in _budgets:
        start = time.time()
        sample, timings = rainbow.query_bbox(_long0, _lat0, _long1, _lat1, _zoom, budget=budget*1000, algorithm=_algorithm, keyword=_keyword)
        end = time.time()
        print("final result size: " + str(len(sample)))
        print("total time: " + str(end - start) + " seconds.")
        print("    - query time: " + str(timings["query"]) + " seconds.")
        print("    - decode time: " + str(timings["decode"]) + " seconds.")
        times[str(budget)] = [timings["query"], timings["decode"]]
        file = _data_set + "_" + _keyword + "_" + _algorithm + "_b" + str(budget) + "k.txt"
        rainbow.write2file(sample, file)
    # output times
    print("=========================== Query times ===========================")
    print("data set = " + _data_set)
    print("bbox = [" + str(_long0) + ", " + str(_lat0) + ", " + str(_long1) + ", " + str(_lat1) + "]")
    print("zoom = " + str(_zoom))
    print("algorithm = " + str(_algorithm))
    print("-------------------------------------------------------------------")
    print("budget, query time, decode time")
    for budget in _budgets:
        print(str(budget) + "K, " + str(times[str(budget)][0])
              + ", " + str(times[str(budget)][1]))
    print("====================================================================")
    # write times to csv
    csv_file_name = _data_set + "_" + _keyword + "_" + _algorithm + "_time_budgets.csv"
    with open(csv_file_name, 'w') as csv_file:
        csv_writer = csv.writer(csv_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        csv_writer.writerow(["budget", "query time", "decode time"])
        for budget in _budgets:
            csv_writer.writerow([str(budget) + "K",
                                 str(times[str(budget)][0]),
                                 str(times[str(budget)][1])])


###############################################################
#  computer errors of different _budgets
#   - 20K, 40K, 60K, 80K, 100K
###############################################################
def errors_of_different_budgets(_data_set, _budgets, _algorithm, _keyword):
    # output times
    print("=========================== Rendering Errors ===========================")
    print("data set = " + _data_set)
    print("algorithm = " + str(_algorithm))
    print("------------------------------------------------------------------------")
    print("budget, error")
    errors = {}
    for budget in _budgets:
        superset_file = _data_set + "_" + _keyword + "_" + _algorithm + "_all.txt"
        subset_file = _data_set + "_" + _keyword + "_" + _algorithm + "_b" + str(budget) + "k.txt"
        error = compare_points_sets(superset_file, subset_file)
        errors[str(budget)] = error
        print(str(budget) + "k, " + "{}".format(error))
    print("========================================================================")
    # write errors to csv
    csv_file_name = _data_set + "_" + _keyword + "_" + _algorithm + "_errors_budgets.csv"
    with open(csv_file_name, 'w') as csv_file:
        csv_writer = csv.writer(csv_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        csv_writer.writerow(["budget", "error"])
        for budget in _budgets:
            csv_writer.writerow([str(budget) + "K",
                                 str(errors[str(budget)])
                                 ])


# data set
data_set = "10m"

keyword = "earthquake"

# bbox
long0 = -138.515625
lat0 = 21.08450008351735
long1 = -54.140625
lat1 = 54.08517342088679
zoom = 5

# budget
budgets = range(20, 120, 20)  # 20K, 40K, 60K, 80K, 100K

# 1 run RAQuadTree (Euclidean Distance as error metric) varying budgets
algorithm = "RAQuadTreeDistance"
warm_up(algorithm, keyword)
query_ground_truth(data_set, long0, lat0, long1, lat1, zoom, algorithm, keyword)
query_different_budgets(data_set, long0, lat0, long1, lat1, zoom, budgets, algorithm, keyword)

# 2 run KDTreeExplorer varying budgets
algorithm = "KDTreeExplorer"
warm_up(algorithm, keyword)
query_ground_truth(data_set, long0, lat0, long1, lat1, zoom, algorithm, keyword)
query_different_budgets(data_set, long0, lat0, long1, lat1, zoom, budgets, algorithm, keyword)

rainbow.close()

#  3 compute errors for RAQuadTree varying budgets
algorithm = "RAQuadTreeDistance"
errors_of_different_budgets(data_set, budgets, algorithm, keyword)

#  4 compute errors for KDTreeExplorer varying budgets
algorithm = "KDTreeExplorer"
errors_of_different_budgets(data_set, budgets, algorithm, keyword)

