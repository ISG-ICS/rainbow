if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

var app = angular.module("rainbow", ["rainbow.map", "rainbow.searchbar"]);

app.controller("AppCtrl", function ($scope) {
  console.log("Leaflet version = " + L.version);
});