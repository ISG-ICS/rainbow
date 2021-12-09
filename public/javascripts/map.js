angular.module("rainbow.map", ["leaflet-directive", "rainbow.common"])
  .controller("MapCtrl", function($scope, $timeout, leafletData, moduleManager) {

    $scope.scatterType = "leaflet"; // "leaflet" / "deck-gl" / "gl-pixel" / "gl-raster"
    $scope.pointRadius = 2.5; // default point radius
    $scope.opacity = 1.0; // default opacity

    // timing for query
    $scope.queryTimings = {
      zoom: 4,
      serverTime: 0.0,
      treeTime: 0.0,
      aggregateTime: 0.0,
      networkTime: 0.0,
      renderTime: 0.0,
      resultSize: 0.0
    };
    // timing for query
    $scope.queryStart = 0.0;
    // timing for rendering
    $scope.renderStart = 0.0;

    $scope.printQueryTimings = function() {
      console.log("-------- Query Timings (csv) --------");
      console.log("zoom,    serverTime,    treeTime,    aggregateTime,    networkTime,    renderTime,    resultSize");
      console.log($scope.queryTimings.zoom + ",    " +
        $scope.queryTimings.serverTime + ",    " +
        $scope.queryTimings.treeTime + ",    " +
        $scope.queryTimings.aggregateTime + ",    " +
        $scope.queryTimings.networkTime + ",    " +
        $scope.queryTimings.renderTime + ",    " +
        $scope.queryTimings.resultSize
      );
      console.log("-------------------------------------");
    };

    // wsFormat
    $scope.wsFormat = "binary"; // "array" (json) / "binary"

    // store request object for handle websocket onMessage
    $scope.request = {};

    // store query object
    $scope.query = {
      key: "",
      zoom: 0,
      bbox: [],
      algorithm: "",
      resX: 1920,
      resY: 978,
      aggregator: "RAQuadTree",
      sampleSize: 0
    };

    $scope.ws = new WebSocket("ws://" + location.host + "/ws");
    $scope.ws.binaryType = 'arraybuffer';

    /** middleware mode */
    $scope.sendQuery = function(e) {
      console.log("e = " + JSON.stringify(e));

      if (e.keyword) {
        $scope.query.keyword = e.keyword;
      }

      if ($scope.map) {
        $scope.query.zoom = $scope.map.getZoom();
        $scope.query.bbox = [
          $scope.map.getBounds().getWest(),
          $scope.map.getBounds().getSouth(),
          $scope.map.getBounds().getEast(),
          $scope.map.getBounds().getNorth()
        ];

        $scope.query.resX = $scope.map.getSize().x;
        $scope.query.resY = $scope.map.getSize().y;
      }

      if (e.algorithm) {
        $scope.query.algorithm = e.algorithm;
      }

      if (e.sampleSize) {
        $scope.query.sampleSize = e.sampleSize;
      }

      $scope.query.aggregator = $scope.scatterType;

      // only send query when comprised query has enough information, i.e. keyword, algorithm
      if ($scope.query.keyword && $scope.query.algorithm) {
        $scope.query.key = $scope.query.keyword + "-" + $scope.query.algorithm;

        let request = {
          type: "query",
          keyword: $scope.query.keyword,
          query: $scope.query
        };

        console.log("sending query:");
        console.log(JSON.stringify(request));

        // timing query
        $scope.queryStart = performance.now();

        $scope.ws.send(JSON.stringify(request));

        $scope.request = request;

        document.getElementById("myBar").style.width = "0%";
      }
    };

    $scope.waitForWS = function() {

      if ($scope.ws.readyState !== $scope.ws.OPEN) {
        window.setTimeout($scope.waitForWS, 1000);
      }
      else {
        moduleManager.publishEvent(moduleManager.EVENT.WS_READY, {});

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, function(e) {
          $scope.cleanScatterLayer();
          $scope.sendQuery(e);
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, function(e) {
          $scope.sendQuery(e);
        });

        $scope.map.on('moveend', function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {zoom: $scope.map.getZoom()});
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SAMPLE_SIZE, function(e) {
          $scope.sendQuery(e);
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_POINT_RADIUS, function(e) {
          console.log("switch point radius to " + e.pointRadius);
          $scope.pointRadius = e.pointRadius;
          $scope.cleanScatterLayer();
          if ($scope.points) {
            $scope.drawScatterLayer($scope.points);
          }
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_OPACITY, function(e) {
          console.log("switch opacity to " + e.opacity);
          $scope.opacity = e.opacity;
          $scope.cleanScatterLayer();
          if ($scope.points) {
            $scope.drawScatterLayer($scope.points);
          }
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SCATTER_TYPE, function(e) {
          console.log("switch scatter type to " + e.scatterType);
          $scope.cleanScatterLayer();
          $scope.scatterType = e.scatterType;
          if ($scope.points) {
            $scope.drawScatterLayer($scope.points);
          }
        });

      }
    };

    // setting default map styles, zoom level, etc.
    /** open street map light style */
    angular.extend($scope, {
      tiles: {
        name: 'OpenStreetMap',
        url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.pn',
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, &copy; <a href="https://carto.com/attribution">CARTO</a>'
      },
      controls: {
        custom: []
      }
    });

    // initialize the leaflet map
    $scope.init = function() {
      leafletData.getMap().then(function (map) {
        $scope.map = map;
        $scope.bounds = map.getBounds();
        //making attribution control to false to remove the default leaflet sign in the bottom of map
        map.attributionControl.setPrefix(false);
        map.setView([$scope.lat, $scope.lng], $scope.zoom);
      });

      //Reset Zoom Button
      var button = document.createElement("a");
      var text = document.createTextNode("Reset");
      button.appendChild(text);
      button.title = "Reset";
      button.style.position = 'fixed';
      button.style.top = '70px';
      button.style.left = '8px';
      button.style.fontSize = '14px';
      var body = document.body;
      body.appendChild(button);
      button.addEventListener("click", function () {
        $scope.map.setView([$scope.lat, $scope.lng], 4, {animate: true});
      });

      $scope.waitForWS();

      // create screenshot button
      $scope.screenshotButton = document.createElement("button");
      $scope.screenshotButton.className = "float";
      $scope.screenshotButton.id = "screenshot-btn";
      $scope.screenshotButton.innerHTML = "Screenshot";
      $scope.screenshotButton.style.bottom = "20px";
      $scope.screenshotButton.style.right = "20px";
      $scope.screenshotButton.style.width = "90px";
      document.body.appendChild($scope.screenshotButton);
      $scope.screenshotButton.addEventListener("click", function() {
        if ($scope.scatterType === "deck-gl" && $scope.scatterLayer) {
          $scope.scatterLayer.redraw(true);
        }
        let div = document.getElementById("deck-gl-canvas");
        html2canvas(div).then(canvas => {
          document.body.appendChild(canvas);
          let a = document.createElement('a');
          // toDataURL defaults to png, so we need to request a jpeg, then convert for file download.
          a.href = canvas.toDataURL("image/png").replace("image/png", "image/octet-stream");
          a.download = 'screenshot.png';
          a.click();
        });
      });

    };

    /** middleware mode */
    $scope.handleResult = function(result) {
      if(result.data.length > 0) {
        $scope.pointsCount = result.data.length;
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_RESULT_COUNT, {pointsCount: $scope.pointsCount});
        $scope.drawScatterLayer(result.data);
      }
    };

    $scope.parseBinary = function(binaryData) {
      // ---- header ----
      //  progress  totalTime  treeTime  aggTime   msgType
      // | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 4 BYTES |
      let dv = new DataView(binaryData);
      let response = {};
      let offset = 0; // offset by bytes
      response.progress = dv.getInt32(offset);
      offset = offset + 4;
      response.totalTime = dv.getFloat64(offset);
      offset = offset + 8;
      response.treeTime = dv.getFloat64(offset);
      offset = offset + 8;
      response.aggregateTime = dv.getFloat64(offset);
      offset = offset + 8;
      response.msgType = dv.getInt32(offset);
      offset = offset + 4;
      const headerSize = 4 + 8 + 8 + 8 + 4;
      // message type = binary
      if (response.msgType == 0) {
        // ---- binary data payload ----
        //   lat1      lng1      lat2      lng2      ...
        // | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | ...
        const recordSize = 8 + 8;
        let dataLength = (dv.byteLength - headerSize) / recordSize;
        let data = [];
        for (let i = 0; i < dataLength; i++) {
          // current record's starting offset
          offset = headerSize + recordSize * i;
          let record = [];
          record.push(dv.getFloat64(offset)); // lat
          offset = offset + 8;
          record.push(dv.getFloat64(offset)); // lng
          data.push(record);
        }
        response.result = {data: data};
        console.log("==== websocket received binary data ====");
        console.log(binaryData);
        console.log("size = " + (headerSize + dataLength * recordSize) / (1024.0 * 1024.0) + " MB.");
        return response;
      }
      // message type = bitmap
      else if (response.msgType === 1) {
        // ---- bitmap header ----
        //   resX      resY      lng0      lat0      lng1      lat1
        // | 4 BYTES | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES |
        // ---- bitmap data payload ----
        //   i=0, j=0~_resY          i=1, j=0~_resY        ...
        // | ceil(resY / 8) BYTES | ceil(resY / 8) BYTES | ...
        const resX = dv.getInt32(offset);
        offset = offset + 4;
        const resY = dv.getInt32(offset);
        offset = offset + 4;
        const lng0 = dv.getFloat64(offset);
        offset = offset + 8;
        const lat0 = dv.getFloat64(offset);
        offset = offset + 8;
        const lng1 = dv.getFloat64(offset);
        offset = offset + 8;
        const lat1 = dv.getFloat64(offset);
        offset = offset + 8;
        const bitmapHeaderSize = 4 * 2 + 8 * 4;
        const bitmapOneLineSize = Math.ceil(resY / 8.0);

        // functions for mercator projection
        function lngX(lng) {
          return lng / 360 + 0.5;
        }

        function latY(lat) {
          let sin = Math.sin(lat * Math.PI / 180);
          let y = (0.5 - 0.25 * Math.log((1 + sin) / (1 - sin)) / Math.PI);
          return y < 0 ? 0 : y > 1 ? 1 : y;
        }

        function xLng(x) {
          return (x - 0.5) * 360;
        }

        function yLat(y) {
          let y2 = (180 - y * 360) * Math.PI / 180;
          return 360 * Math.atan(Math.exp(y2)) / Math.PI - 90;
        }

        const x0 = lngX(lng0);
        const y1 = latY(lat0);
        const x1 = lngX(lng1);
        const y0 = latY(lat1);
        const deltaX = x1 - x0;
        const deltaY = y1 - y0;
        let data = [];
        for (let i = 0; i < resX; i ++) {
          for (let j = 0; j < resY; j ++) {
            let offset = headerSize + bitmapHeaderSize + i * bitmapOneLineSize; // line head
            offset = offset + Math.floor(j / 8); // byte within the line
            let byte = dv.getInt8(offset);
            let pos = j % 8; // pos within byte
            let shift = 7 - pos;
            let bit = (byte >>> shift) & 1;
            // point exists for pixel [i, j]
            if (bit !== 0) {
              let x = (i + 0.5) * deltaX / resX + x0;
              let y = (j + 0.5) * deltaY / resY + y0;
              let lng = xLng(x);
              let lat = yLat(y);
              data.push([lat, lng]);
            }
          }
        }
        response.result = {data: data};
        console.log("==== websocket received binary data ====");
        console.log(binaryData);
        response.resultSize = (headerSize + bitmapHeaderSize + resX * bitmapOneLineSize) / (1024.0 * 1024.0);
        console.log("size = " + response.resultSize + " MB.");
        return response;
      }
    };

    $scope.ws.onmessage = function(event) {
      $timeout(function() {

        // timing for actions
        let queryEnd = performance.now();
        let queryTime = (queryEnd - $scope.queryStart) / 1000.0; // seconds

        let response;
        switch ($scope.request.type) {
          case "query":
            response = $scope.parseBinary(event.data);
            break;
          default:
            response = JSON.parse(event.data);
            break;
        }

        /**
         * response: {
         *   result: {
         *     data: [
         *       [lat1, lng1, count1],
         *       [lat2, lng2, count2],
         *       ...
         *     ]
         *   }
         * }
         */
        console.log("===== websocket response =====");
        console.log(JSON.stringify(response));

        // keep notes of timings
        $scope.queryTimings.zoom = $scope.query.zoom;
        $scope.queryTimings.serverTime = response.totalTime;
        $scope.queryTimings.treeTime = response.treeTime;
        $scope.queryTimings.aggregateTime = response.aggregateTime;
        $scope.queryTimings.networkTime = queryTime - response.totalTime;
        $scope.queryTimings.resultSize = response.resultSize;
        if ($scope.timeActions) {
          $scope.actionTime = {
            zoom: $scope.queryTimings.zoom,
            serverTime: $scope.queryTimings.serverTime,
            treeTime: $scope.queryTimings.treeTime,
            aggregateTime: $scope.queryTimings.aggregateTime,
            networkTime: $scope.queryTimings.networkTime,
            resultSize: $scope.queryTimings.resultSize
          };
        }

        switch ($scope.request.type) {
          case "query":
            $scope.handleResult(response.result);
            if (typeof response.progress == "number") {
              document.getElementById("myBar").style.width = response.progress + "%";
            }
            break;
        }
      });
    };
    
    /** middleware mode */
    // function for drawing scatter plot layer
    $scope.drawScatterLayer = function(data) {
      // timing for rendering
      $scope.renderStart = performance.now();

      // initialize the scatter layer
      if (!$scope.scatterLayer) {
        let pointRadius = $scope.pointRadius;
        switch ($scope.scatterType) {
          case "gl-pixel":
            $scope.scatterLayer = new WebGLPointLayer({renderMode: "pixel"}); // renderMode: raster / pixel
            $scope.scatterLayer.setPointSize(2 * pointRadius);
            $scope.scatterLayer.setPointColor(0, 0, 255);
            $scope.map.addLayer($scope.scatterLayer);
            break;
          case "gl-raster":
            $scope.scatterLayer = new WebGLPointLayer({renderMode: "raster"}); // renderMode: raster / pixel
            $scope.scatterLayer.setPointSize(2 * pointRadius);
            $scope.scatterLayer.setPointColor(0, 0, 255);
            $scope.map.addLayer($scope.scatterLayer);
            break;
          case "leaflet":
            $scope.scatterLayer = L.TileLayer.maskCanvas({
              radius: pointRadius,  // radius in pixels or in meters (see useAbsoluteRadius)
              useAbsoluteRadius: false,  // true: r in meters, false: r in pixels
              color: 'blue',  // the color of the layer
              opacity: 1.0,  // opacity of the not covered area
              noMask: true//,  // true results in normal (filled) circled, instead masked circles
              //lineColor: 'blue'   // color of the circle outline if noMask is true
            });
            $scope.map.addLayer($scope.scatterLayer);
            break;
          case "deck-gl":
            $scope.scatterLayer = new deck.DeckGL({
              container: 'deck-gl-canvas',
              initialViewState: {
                latitude: $scope.map.getCenter().lat,
                longitude: $scope.map.getCenter().lng,
                zoom: $scope.map.getZoom() - 1,
                bearing: 0,
                pitch: 0
              },
              controller: true,
              layers: []
            });
            break;
        }
        $scope.points = [];
      }

      // update the scatter layer
      if (data.length > 0) {
        $scope.points = data; // [lat, lng]
        console.log("[draw scatterplot] drawing points size = " + data.length);
        // redraw scatter layer
        switch ($scope.scatterType) {
          case "gl-pixel":
          case "gl-raster":
            // construct consumable points array for scatter layer
            let points = [];
            for (let i = 0; i < data.length; i ++) {
              let point = data[i];
              points.push([point[0], point[1], i]);
            }
            $scope.scatterLayer.setData(points);
            break;
          case "leaflet":
            $scope.scatterLayer.setData($scope.points);
            break;
          case "deck-gl":
            const deckGLScatterplot = new deck.ScatterplotLayer({
              /* unique id of this layer */
              id: 'deck-gl-scatter',
              data: $scope.points,
              opacity: $scope.opacity,
              /* data accessors */
              radiusMinPixels: Math.round($scope.pointRadius),
              getPosition: d => [d[1], d[0]],     // returns longitude, latitude, [altitude]
              getRadius: d => $scope.pointRadius,  // returns radius in meters
              getFillColor: d => [0, 0, 255]           // returns R, G, B, [A] in 0-255 range
            });
            $scope.scatterLayer.setProps(
              {
                layers: [deckGLScatterplot],
                viewState:
                  {
                    latitude: $scope.map.getCenter().lat,
                    longitude: $scope.map.getCenter().lng,
                    zoom: $scope.map.getZoom() - 1,
                    bearing: 0,
                    pitch: 0
                  }
              });
            console.log("==== view state for deck.lg ====");
            console.log("latitude: " + $scope.map.getCenter().lat);
            console.log("longitude: " + $scope.map.getCenter().lng);
            console.log("zoom: " + ($scope.map.getZoom() - 1));
            console.log("bearing: " + 0);
            console.log("pitch: " + 0);
            break;
        }
      }

      // timing for rendering
      let renderEnd = performance.now();
      $scope.queryTimings.renderTime = (renderEnd - $scope.renderStart) / 1000.0; // seconds
      $scope.printQueryTimings();
      if ($scope.timeActions) {
        $scope.actionTime.renderTime = $scope.queryTimings.renderTime;
        $scope.actionTimings.push($scope.actionTime);
      }
      if ($scope.replaying) {
        moduleManager.publishEvent(moduleManager.EVENT.FINISH_ACTION, {});
      }
    };

    /** middleware mode */
    $scope.cleanScatterLayer = function () {
      if ($scope.scatterLayer) {
        if ($scope.scatterType === "deck-gl") {
          $scope.scatterLayer.setProps({layers: []});
        }
        else {
          $scope.map.removeLayer($scope.scatterLayer);
        }
        $scope.scatterLayer = null;
      }
    };

  })
  .directive("map", function () {
    return {
      restrict: 'E',
      scope: {
        lat: "=",
        lng: "=",
        zoom: "="
      },
      controller: 'MapCtrl',
      template:[
        '<leaflet lf-center="center" tiles="tiles" events="events" controls="controls" width="100%" height="100%" ng-init="init()"></leaflet>'
      ].join('')
    };
  });


angular.module("rainbow.map")
  .controller('CountCtrl', function($scope, moduleManager) {
    $scope.resultCount = "";

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_RESULT_COUNT, function(e) {
      if (e.resultCount) {
        $scope.resultCount = e.resultCount + ": " + e.pointsCount;
      }
      else {
        $scope.resultCount = e.pointsCount;
      }
    })
  });