const app = angular.module('mafleurmavalise', []);
const socket = io.connect({transports:['polling']});

/**
 * Contrôleur AngularJs V1
 */
app.controller('statsCtrl', function($scope){

  $scope.votes = []

  const updateScores = function(){
    socket.on('scores', function (json) {
       data = JSON.parse(json);
       
       $scope.$apply(function () {
         $scope.votes = data;
       });
    });
  };

  const init = function(){
    document.body.style.opacity=1;
    updateScores();
  };
  socket.on('message',function(data){
    init();
  });
});