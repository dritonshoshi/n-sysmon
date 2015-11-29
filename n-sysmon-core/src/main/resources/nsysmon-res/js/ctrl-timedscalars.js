angular.module('NSysMonApp').controller('CtrlTimedScalars', function($scope, $timeout, $log, Rest, formatNumber, startsWith) {
    $scope.options =  {
      "chart": {
            "type": "lineWithFocusChart",
            "height": 450,
            "margin": {
              "top": 20,
              "right": 20,
              "bottom": 60,
              "left": 60
            },
//            x: function(d){ return d[0]; },
//            y: function(d){ return d[1]/100; },
            color: d3.scale.category10().range(),
            useInteractiveGuideline: true,
            clipVoronoi: false,
            objectEquality : false,
            "transitionDuration": 500,
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

    $scope.autoRefresh = false;
    $scope.autoRefreshSeconds = 1; //TODO change this value to something usefull
    var autoRefreshCounter = 0; // to invalidate auto-refresh if there was a manual refresh in between

    function initGraphDataFromResponse(data) {
        $scope.graphData = data;
        $scope.rc.api.updateWithData(data);
        triggerAutoRefresh();
    }

    $scope.$watch('autoRefresh', triggerAutoRefresh);
    $scope.$watch('autoRefreshSeconds', triggerAutoRefresh);

    function triggerAutoRefresh() {
        if(! $scope.autoRefresh) {
            return;
        }

        var oldCounter = autoRefreshCounter;
        setTimeout(function() {
            if(autoRefreshCounter !== oldCounter+1) {
                return;
            }
            $scope.refresh();
        }, $scope.autoRefreshSeconds * 1000);
        autoRefreshCounter += 1;
    }

    $scope.refresh = function() {
        selectedEntries = "";
        if (typeof $scope.timedScalars == 'undefined'){
            return;
        }
        for (timedScalar in $scope.timedScalars) {
            if ($scope.timedScalars[timedScalar].selected){
                selectedEntries = selectedEntries.concat($scope.timedScalars[timedScalar].key);
                selectedEntries = selectedEntries.concat(",");
            }
        }
        if (selectedEntries.length > 1) {
            Rest.call('getGraphData/' + selectedEntries, initGraphDataFromResponse);
        }

    };

    function initFromResponse(data) {
        $scope.timedScalars = data.timedScalars;
        triggerAutoRefresh();
    }

    $scope.toggleGraphData = function(key) {
        $scope.timedScalars[key].selected = !$scope.timedScalars[key].selected;
        $scope.refresh();
    }

    Rest.call('getData', initFromResponse);
    $scope.refresh();
});

