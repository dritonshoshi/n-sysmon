var nsysmonApp = angular.module('nsysmon-app', [
    'ngRoute',
    'nsysmonControllers'
]);

nsysmonApp.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/start', {
                templateUrl: 'partials/start.html',
                controller: 'StartCtrl'
            }).
//            when('/phones/:phoneId', {
//                templateUrl: 'partials/phone-detail.html',
//                controller: 'PhoneDetailCtrl'
//            }).
            otherwise({
                redirectTo: '/start'
            });
    }]);
