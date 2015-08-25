
angular.module('NSysMonApp', ['ngRoute', 'nsysmon'], function($routeProvider, configRaw) {
    angular.forEach(configRaw.menuEntries, function(menuEntry) {
        angular.forEach(menuEntry.entries, function(pageDef) {
            $routeProvider.when('/' + pageDef.id, {templateUrl: '_$_nsysmon_$_/static/partials/' + pageDef.htmlFileName, controller: pageDef.controller});
        });
    });

    $routeProvider.otherwise({ redirectTo: '/' + configRaw.defaultPage });
});

angular.module('NSysMonApp').controller('NSysMonCtrl', function($scope, $route, $location, config) {
    $scope.configRaw = config.raw();
    $scope.curTitle = function() {
        return config.forCurrentPage().fullLabel;
    };

    $scope.applicationIdentifier = function() {
        return config.raw().applicationId + " " + config.raw().applicationVersion + " [" + config.raw().applicationNode + '] on ' + config.raw().applicationDeployment;
    };

    $scope.applicationColor = function() {
        return config.raw().applicationInstanceHtmlColorCode;
    };
});