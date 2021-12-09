angular.module("rainbow.searchbar", ["rainbow.common"])
    .controller("SearchCtrl", function ($scope, moduleManager) {
        $scope.disableSearchButton = true;

        $scope.search = function () {
            if ($scope.keyword && $scope.keyword.trim().length > 0) {
                moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,
                    {
                      keyword: $scope.keyword,
                      algorithm: $scope.algorithm
                    });
            }
        };

        moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, function(e) {
            $scope.disableSearchButton = false;
        });

        $scope.algorithms = ["RAQuadTree", "RAQuadTreeDistance", "QuadTreeExplorer", "KDTreeExplorer"];
        $scope.scatterTypes = ["leaflet", "deck-gl", "gl-pixel", "gl-raster", ];

        /** Left side controls */

        // Scatter Type Select
        $scope.selectScatterTypes = document.createElement("select");
        $scope.selectScatterTypes.id = "scatterTypes";
        $scope.selectScatterTypes.title = "scatterTypes";
        $scope.selectScatterTypes.style.position = 'fixed';
        $scope.selectScatterTypes.style.top = '125px';
        $scope.selectScatterTypes.style.left = '8px';
        for (let i = 0; i < $scope.scatterTypes.length; i ++) {
          let option = document.createElement("option");
          option.text = $scope.scatterTypes[i];
          $scope.selectScatterTypes.add(option);
        }
        $scope.selectScatterTypes.value = $scope.scatterTypes[0];
        document.body.appendChild($scope.selectScatterTypes);
        $scope.selectScatterTypes.addEventListener("change", function () {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SCATTER_TYPE,
            {scatterType: $scope.selectScatterTypes.value});
        });
        $scope.selectScatterTypesLabel = document.createElement("label");
        $scope.selectScatterTypesLabel.innerHTML = "Scatter Lib";
        $scope.selectScatterTypesLabel.htmlFor ="scatterLib";
        $scope.selectScatterTypesLabel.style.position = 'fixed';
        $scope.selectScatterTypesLabel.style.top = '125px';
        $scope.selectScatterTypesLabel.style.left = '90px';
        document.body.appendChild($scope.selectScatterTypesLabel);

        // Point Radius Select
        $scope.selectPointRadius = document.createElement("select");
        $scope.selectPointRadius.title = "pointRadius";
        $scope.selectPointRadius.style.position = 'fixed';
        $scope.selectPointRadius.style.top = '150px';
        $scope.selectPointRadius.style.left = '8px';
        for (let i = 0.5; i <=5 ; i += 0.5) {
          let option = document.createElement("option");
          option.text = "" + i;
          $scope.selectPointRadius.add(option);
        }

        $scope.selectPointRadius.value = "2.5";
        document.body.appendChild($scope.selectPointRadius);
        $scope.selectPointRadius.addEventListener("change", function () {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_POINT_RADIUS,
            {pointRadius: $scope.selectPointRadius.value});
        });
        $scope.selectPointRadiusLabel = document.createElement("label");
        $scope.selectPointRadiusLabel.innerHTML = "Point Radius";
        $scope.selectPointRadiusLabel.htmlFor ="pointRadius";
        $scope.selectPointRadiusLabel.style.position = 'fixed';
        $scope.selectPointRadiusLabel.style.top = '150px';
        $scope.selectPointRadiusLabel.style.left = '60px';
        document.body.appendChild($scope.selectPointRadiusLabel);

        // Opacity Select
        $scope.selectOpacity = document.createElement("select");
        $scope.selectOpacity.title = "opacity";
        $scope.selectOpacity.style.position = 'fixed';
        $scope.selectOpacity.style.top = '175px';
        $scope.selectOpacity.style.left = '8px';
        for (let i = 1; i <= 9; i ++) {
          let option = document.createElement("option");
          option.text = "0." + i;
          $scope.selectOpacity.add(option);
        }
        let option = document.createElement("option");
        option.text = "1.0";
        $scope.selectOpacity.add(option);

        $scope.selectOpacity.value = "1.0";
        document.body.appendChild($scope.selectOpacity);
        $scope.selectOpacity.addEventListener("change", function () {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_OPACITY,
            {opacity: $scope.selectOpacity.value});
        });
        $scope.selectOpacityLabel = document.createElement("label");
        $scope.selectOpacityLabel.innerHTML = "Opacity";
        $scope.selectOpacityLabel.htmlFor ="opacity";
        $scope.selectOpacityLabel.style.position = 'fixed';
        $scope.selectOpacityLabel.style.top = '175px';
        $scope.selectOpacityLabel.style.left = '60px';
        document.body.appendChild($scope.selectOpacityLabel);

        // Sample Size Select
        $scope.selectSampleSize = document.createElement("select");
        $scope.selectSampleSize.title = "SampleSize";
        $scope.selectSampleSize.style.position = 'fixed';
        $scope.selectSampleSize.style.top = '200px';
        $scope.selectSampleSize.style.left = '8px';

        option = document.createElement("option");
        option.text = "0";
        option.value = "0";
        $scope.selectSampleSize.add(option);
        for (let i = 1; i < 10; i += 1) {
          let option = document.createElement("option");
          option.text = i + "K";
          option.value = "" + i * 1000;
          $scope.selectSampleSize.add(option);
        }
        for (let i = 10; i <= 100; i += 10) {
          let option = document.createElement("option");
          option.text = i + "K";
          option.value = "" + i * 1000;
          $scope.selectSampleSize.add(option);
        }
        for (let i = 200; i <= 900; i += 100) {
          let option = document.createElement("option");
          option.text = i + "K";
          option.value = "" + i * 1000;
          $scope.selectSampleSize.add(option);
        }
        for (let i = 1.0; i <= 5.0; i += 0.5) {
          let option = document.createElement("option");
          option.text = "" + i + "M";
          option.value = "" + Number(i * 1000000);
          $scope.selectSampleSize.add(option);
        }

        $scope.selectSampleSize.value = "0";
        document.body.appendChild($scope.selectSampleSize);
        $scope.selectSampleSize.addEventListener("change", function () {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SAMPLE_SIZE,
            {sampleSize: $scope.selectSampleSize.value});
        });
        $scope.selectSampleSizeLabel = document.createElement("label");
        $scope.selectSampleSizeLabel.innerHTML = " Sample Size";
        $scope.selectSampleSizeLabel.htmlFor ="SampleSize";
        $scope.selectSampleSizeLabel.style.position = 'fixed';
        $scope.selectSampleSizeLabel.style.top = '200px';
        $scope.selectSampleSizeLabel.style.left = '68px';
        document.body.appendChild($scope.selectSampleSizeLabel);

    })
    .directive("searchBar", function () {
        return {
            restrict: "E",
            controller: "SearchCtrl",
            template: [
                '<form class="form-inline" id="input-form" ng-submit="search()" >',
                '  <div class="input-group col-lg-12">',
                '    <label class="sr-only">Keywords</label>',
                '    <input type="text" style="width: 97%" class="form-control " id="keyword-textbox" placeholder="Search keywords, e.g. hurricane" ng-model="keyword"/>',
                '    <span class="input-group-btn">',
                '      <button type="submit" class="btn btn-primary" id="submit-button" ng-disabled="disableSearchButton">Submit</button>',
                '    </span>',
                '  </div>',
                '  <div id="myProgress" class="input-group col-lg-12" style="width: 69%">',
                '    <div id="myBar"></div>',
                '  </div>',
                '</form>',
                '<label for="algorithm">Algorithm</label>&nbsp;<select id="algorithm" ng-model="algorithm" ng-options="x for x in algorithms" ng-init="algorithm = algorithms[0]"></select>&nbsp;'
            ].join('')
        };
    });