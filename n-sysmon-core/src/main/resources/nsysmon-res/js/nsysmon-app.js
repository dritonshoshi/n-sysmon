
angular.module('NSysMonApp', ['ngRoute', 'nsysmon', 'nvd3', 'angular.filter'], function($routeProvider, configRaw) {

    angular.forEach(configRaw.menuEntries, function(menuEntry) {
        angular.forEach(menuEntry.entries, function(pageDef) {
            $routeProvider.when('/' + pageDef.id, {templateUrl: '_$_nsysmon_$_/static/partials/' + pageDef.htmlFileName, controller: pageDef.controller});
        });
    });

    $routeProvider.otherwise({ redirectTo: '/' + configRaw.defaultPage });
});

angular.module('NSysMonApp').controller('NSysMonCtrl', function($scope, $route, $location, config) {
    $scope.rc = {};
    $scope.configRaw = config.raw();
    $scope.curTitle = function() {
        if (!config.forCurrentPage()){
            return "unknown";
        }else{
            return config.forCurrentPage().fullLabel;
        }
    };

    $scope.applicationIdentifier = function() {
        return config.raw().applicationId + " " + config.raw().applicationVersion + " [" + config.raw().applicationNode + '] on ' + config.raw().applicationDeployment;
    };

    $scope.applicationColor = function() {
        return config.raw().applicationInstanceHtmlColorCode;
    };
});

// filter to be able to sort objects by variable-name and not only arrays
angular.module('NSysMonApp').filter('timedScalarEntrySorter', function() {
    return function(items, reverse) {
        var filtered = [];
        angular.forEach(items, function(item) {
            filtered.push(item);
        });
        filtered.sort(function (entryA, entryB) {
                if (entryA.group == entryB.group){
                    return (entryA.key > entryB.key ? 1 : -1);
                }
                return (entryA.group > entryB.group ? 1 : -1);
        });
        if(reverse) filtered.reverse();
        return filtered;
    };
});
