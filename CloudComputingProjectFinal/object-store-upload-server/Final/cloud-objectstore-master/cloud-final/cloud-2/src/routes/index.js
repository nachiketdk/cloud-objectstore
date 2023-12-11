const express = require("express");
const router = express.Router();
const controller = require("../controller/file.controller");

let routes = (app) => {
  router.post("/putFile", controller.putFile);
  router.get("/getFile", controller.getFile);

  app.use(router);
};

module.exports = routes;
