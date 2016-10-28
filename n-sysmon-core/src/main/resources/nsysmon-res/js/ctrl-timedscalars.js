angular.module('NSysMonApp').controller('CtrlTimedScalars', function($scope, $timeout, $log, Rest, $location) {
    $scope.options =  {
      "chart": {
            "type": "lineWithFocusChart",
            "height": 500,
            "margin": {
              "top": 20,
              "right": 20,
              "bottom": 20,
              "left": 40
            },
            color: d3.scale.category10().range(),
            useInteractiveGuideline: true,
            clipVoronoi: true,
            objectEquality : false,
            "transitionDuration": 1000,
            "xAxis": {
               tickFormat: function(d) {
                    return d3.time.format('%m.%d %H:%M:%S')(new Date(d))
                },
                showMaxMin: false,
                axisLabel: "Time"
            },
            "x2Axis": {
               tickFormat: function(d) {
                    return d3.time.format('%m.%d %H:%M:%S')(new Date(d))
                },
                showMaxMin: false
            },
            "yAxis": {
              "axisLabel": "Value",
              "rotateYLabel": true,
            },
            "y2Axis": {
              "rotateYLabel": true,
            }
        }
    };

    $scope.config = {
        visible: true, // default: true
        extended: true, // default: false
        disabled: false, // default: false
        autorefresh: true, // default: true
        refreshDataOnly: true // default: false
    };

    $scope.graphData = []; //can leave empty

    $('title').text("NSysmon - Timed Scalars");

    $scope.activePage = "";
    $scope.autoRefresh = true;
    $scope.autoRefreshSeconds = 120;
    $scope.useFilterMinutes = false;
    $scope.displayFilterMinutes = 5;
    $scope.entriesToLoadDataFor = [];
    // to invalidate auto-refresh if there was a manual refresh in between
    var autoRefreshCounter = 0;

    function initGraphDataFromResponse(data) {
        $scope.loadedGraphData = data;
        if ($location.search().loadfile) {
            $scope.rc.api.updateWithData([]);
            $scope.timedScalars = {};
            selectedEntries = "";

            var variableKey;
            for (var myKey in $scope.loadedGraphData) {
                variableKey = data[myKey].key;
                $scope.timedScalars[variableKey] = {key: variableKey, selected: false};
            }
        }else{
            $scope.rc.api.updateWithData(data);
            refreshButtons()
            triggerAutoRefresh();
        }
    }

    $scope.$watch('autoRefresh', triggerAutoRefresh);
    $scope.$watch('autoRefreshSeconds', triggerAutoRefresh);

    function refreshButtons(){
        for (var myKey in $scope.loadedGraphData) {
            variableKey = $scope.loadedGraphData[myKey].key;
            status = $scope.loadedGraphData[myKey].status;
            // console.log(variableKey);
            // console.log(status);
            $scope.timedScalars[variableKey].status = status;
        }
    }

    function triggerAutoRefresh() {
        if(! $scope.autoRefresh) {
            return;
        }
        if ($location.search().loadfile) {
            return;
        }

        var oldCounter = autoRefreshCounter;
        $scope.activePage = $location.path();
        $timeout(function() {
            if(autoRefreshCounter !== oldCounter+1) {
                return;
            }
            if ($location.path() == $scope.activePage){
                $scope.refresh();
            }
        }, $scope.autoRefreshSeconds * 1000);
        autoRefreshCounter += 1;
    }

    $scope.refresh = function() {
        if ($location.search().loadfile) {
            return;
        }

        var selectedEntriesForServer = "";
        if (typeof $scope.timedScalars == 'undefined'){
            return;
        }

        for (var keySelected in $scope.entriesToLoadDataFor) {
            for (var keyData in $scope.timedScalars) {
                if ($scope.timedScalars[keyData].key == $scope.entriesToLoadDataFor[keySelected]){
                    selectedEntriesForServer = selectedEntriesForServer.concat($scope.entriesToLoadDataFor[keySelected]);
                    selectedEntriesForServer = selectedEntriesForServer.concat(",");
                }
            }
        }
        if (selectedEntriesForServer.length > 1 && $scope.useFilterMinutes && $scope.displayFilterMinutes > 0) {
            Rest.call('getLatestGraphData/' + selectedEntriesForServer + "/" + $scope.displayFilterMinutes, initGraphDataFromResponse);
        } else if (selectedEntriesForServer.length > 1) {
            Rest.call('getGraphData/' + selectedEntriesForServer, initGraphDataFromResponse);
        }else {
            //remove old graph-data
            $scope.rc.api.updateWithData([]);
        }

    };

    function initFromResponse(data) {
        $scope.timedScalars = data.timedScalars;
        triggerAutoRefresh();
    }

    $scope.toggleGraphData = function(key) {
        var found = false;
        for (var myKey in $scope.entriesToLoadDataFor) {
            if ($scope.entriesToLoadDataFor[myKey] == key){
                //$scope.entriesToLoadDataFor.remove(Number(myKey));
                $scope.entriesToLoadDataFor.splice(myKey ,1);
                found = true;
            }
        }
        if (!found) {
            $scope.entriesToLoadDataFor.push(key);
        }

        if ($location.search().loadfile) {
            var newGraphData = [];
            for (var myKey in $scope.loadedGraphData) {
                //$scope.entriesToLoadDataFor.push(key);
                var keyName = $scope.loadedGraphData[myKey].key;

                var found = false;
                for (var key in $scope.entriesToLoadDataFor) {
                    if ($scope.entriesToLoadDataFor[key] == $scope.loadedGraphData[myKey].key){
                        //$scope.entriesToLoadDataFor.remove(Number(myKey));
                        found = true;
                    }
                }

                if (found) {
                    newGraphData.push({
                        key: $scope.loadedGraphData[myKey].key,
                        values: $scope.loadedGraphData[myKey].values
                    });
                    // newGraphData.push($scope.graphData[myKey].key, $scope.graphData[myKey].values);
                }
                $scope.graphData = newGraphData;
                $scope.rc.api.updateWithData(newGraphData);
            }
        } else {
            //only, if not loaded from a file
            $scope.refresh();
        }
    };

    // check if data from other sources should be loaded
    function loadExternalData() {
        var loadFileNameParam = $location.search().loadfile;
        if (loadFileNameParam) {
            $scope.entriesToLoadDataFor = [];
            Rest.callOther("loadableServerDataFiles", "loadFromFile" + "/" + loadFileNameParam, initGraphDataFromResponse);
        }else{
            Rest.call('getData', initFromResponse);
            $scope.refresh();
        }
    }
    loadExternalData();

});
