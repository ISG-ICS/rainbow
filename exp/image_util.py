from skimage.metrics import structural_similarity
import numpy as np
import cv2
import matplotlib.pyplot as plt
import json
from scipy import spatial


def mse(grayA, grayB):
    # the 'Mean Squared Error' between the two images is the
    # sum of the squared difference between the two images;
    # NOTE: the two images must have the same dimension
    err = np.sum((grayA.astype("float") - grayB.astype("float")) ** 2)
    if (len(grayA.shape) == 1):
        err /= float(grayA.shape[0])
        #print("count of pixels: " + str(grayA.shape[0]))
    else:
        err /= float(grayA.shape[0] * grayA.shape[1])
        #print("count of pixels: " + str(grayA.shape[0] * grayA.shape[1]))

    # return the MSE, the lower the error, the more "similar"
    # the two images are
    return err


def ssim(grayA, grayB):
    return structural_similarity(grayA, grayB)


# shrink image grayA and grayB to smaller array
# by removing background pixels in grayA (pixel == bgValue)
def no_background(grayA, grayB, bgValue):
    arrayA = np.array(grayA)
    arrayB = np.array(grayB)
    # only care about non-background pixels in grayA
    nonBackgrounIdx = np.where(arrayA != bgValue)
    arrayA = arrayA[nonBackgrounIdx]
    arrayB = arrayB[nonBackgrounIdx]
    print(arrayA)
    print(arrayB)
    return arrayA, arrayB


def show_images(grayA, grayB, title="No Title"):
    # setup the figure
    fig = plt.figure(title)
    # show first image
    ax = fig.add_subplot(1, 2, 1)
    plt.imshow(grayA, cmap = plt.cm.gray)
    plt.axis("off")
    # show the second image
    ax = fig.add_subplot(1, 2, 2)
    plt.imshow(grayB, cmap = plt.cm.gray)
    plt.axis("off")
    # show the images
    plt.show()


def compare_images(file1, file2):
    imageA = cv2.imread(file1)
    imageB = cv2.imread(file2)
    # convert the images to grayscale
    grayA = cv2.cvtColor(imageA, cv2.COLOR_BGR2GRAY)
    grayB = cv2.cvtColor(imageB, cv2.COLOR_BGR2GRAY)
    mse_score = mse(grayA, grayB)
    ssim_score = ssim(grayA, grayB)
    return mse_score, ssim_score


def compare_points_sets(superset_file, subset_file):
    with open(superset_file) as f1:
        superset = json.load(f1)
    with open(subset_file) as f2:
        subset = json.load(f2)
    tree = spatial.KDTree(subset)
    distances, indexes = tree.query(superset)
    error = sum(distances) / len(superset)
    return error


# test
# superset_file = "10m_tweets_nanocube_s100.txt"
# subset_file1 = "/Users/white/Documents/big-spatial-viz/vis2020-short-paper/exps/errors/snapping/job-3m/hd-job-q5-raqtd-b5k.txt"
# subset_file2 = "/Users/white/Documents/big-spatial-viz/vis2020-short-paper/exps/errors/snapping/job-3m/hd-job-q5-raqtd-b10k.txt"
#
# print("5k error = " + "{}".format(compare_points_sets(superset_file, subset_file1)))
# print("10k error = " + "{}".format(compare_points_sets(superset_file, subset_file2)))
