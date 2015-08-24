var nsysmonControllers = angular.module('nsysmonControllers', []);

nsysmonControllers.controller('StartCtrl', ['$scope', '$http',
    function ($scope) {
        $scope.names = ['Ada', 'Berta', 'Caesar'];
    }]);

