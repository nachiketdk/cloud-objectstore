const ConsistentHashing = require("../utils/consistentHashing");
const {
  saveJSONToFile,
  loadJSONFromFile,
  compareVectorClocks,
  filterDataNodes,
} = require("../utils/storageManager");
const request = require("request");
const {
  resolveIfAtLeastPromises,
  resolveIfAtLeastPromisesResult,
} = require("../utils/promises");

const N = process.env.N || 4;
const R = process.env.R || 3;
const W = process.env.W || 3;

// initialize nodesIP
const nodeIPs = {};
for (let i = 0; i < process.env.nodes; i++) {
  nodeIPs[String.fromCharCode(65 + i)] = `http://container${i + 1}:3000`;
}

const selfName = process.env.selfName //comes from env variable

function readRepair(key, otherObject, nodeName) {
  const otherClock = otherObject.vectorClock;
  const otherData = otherObject.data;

  loadJSONFromFile(key, (err, jsonObjects) => {
    const ownClock = jsonObjects[0].vectorClock;
    const ownData = jsonObjects[0].data;

    const compareResult = compareVectorClocks(otherClock, ownClock);

    if (compareResult === "first") {
      //other clock is bigger, repair self
      saveJSONToFile(
        key,
        {
          data: otherData,
        },
        otherClock,
        (err) => {
          if (err) {
            // handle the error
            console.error("Error saving JSON:", err);
          }
        }
      );
      console.log("Repaired data on node", selfName);
    } else if (compareResult === "second") {
      //self clock is bigger, repair other
      makeHttpRequest("POST", nodeName, {
        key: key,
        data: ownData,
        context: ownClock,
        forwarded: "true",
      })
        .then((response) => {
          if (response.statusCode === 200) {
            console.log("Repaired data on node", nodeName);
          }
        })
        .catch((err) => {
          console.log("while repair");
          console.log(err);
        });
    }
  });
}

function makeHttpRequest(method, nodeName, requestBody, forward=false) {
  return new Promise((resolve, reject) => {
    const options = {
      method: method,
      url: `${nodeIPs[nodeName]}/${method === "GET" ? "get" : "put"}File`,
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(requestBody),
    };

    request(options, (error, response) => {
      if (error) {
        reject(new Error(error));
      } else {
        // Optional: Check for non-200 status codes if necessary
        if (response.statusCode === 200) {
          if (method === "POST") {
            resolve({
              statusCode: response.statusCode,
              body: JSON.parse(response.body),
            });
          } else {
            if(forward){
              resolve({
                statusCode: response.statusCode,
                body: JSON.parse(response.body),
              });
            }else{
              const parsedBody = JSON.parse(response.body);
              if (
                Array.isArray(parsedBody.message) &&
                parsedBody.message.length > 0
              ) {
                readRepair(requestBody.key, parsedBody.message[0], nodeName);
              }
              resolve(parsedBody.message);
            }
          }
        } else {
          reject(new Error(response.body));
        }
      }
    });
  });
}

const putFile = async (req, res) => {
  try {
    const key = req.body.key;
    const data = req.body.data;
    const context = req.body.context;
    const forwarded =
      req.body.hasOwnProperty("forwarded") && req.body.forwarded === "true"
        ? true
        : false;

    const toStore = {
      data: data,
    };
    /* example context
    {
      A:1,
      B:2
    }
    */
    const loadbalancer = new ConsistentHashing(
      Object.keys(nodeIPs),
      500,
      "md5"
    ); //use siphash
    const nodeSet = loadbalancer.getNodeset(key);

    if (nodeSet.length === 0) {
      return res.status(500).send({
        message: "No nodes in the ring!",
      });
    } else if (nodeSet.includes(selfName)) {
      // THIS IS THE COORDINATOR NODE
      loadJSONFromFile(key, (err, jsonObjects) => {
        if (err) {
          //NEW KEY
          let newContext = { ...context };
          newContext[selfName] = newContext[selfName] + 1;

          console.log("NEW KEY");

          saveJSONToFile(
            key,
            toStore,
            forwarded ? context : newContext,
            (err) => {
              if (err) {
                // handle the error
                console.error("Error saving JSON:", err);
              }
            }
          );

          if (!forwarded) {
            // FORWARD TO THE OTHER NODES
            let promises = [];

            for (let i = 0; i < nodeSet.length; i++) {
              if (nodeSet[i] === selfName) continue;
              promises.push(
                makeHttpRequest("POST", nodeSet[i], {
                  key: key,
                  data: data,
                  context: newContext,
                  forwarded: "true",
                })
              );
            }

            resolveIfAtLeastPromises(N - 1, W - 1, promises)
              .then((result) => {
                console.log(result);
                return res.status(200).send({
                  message: `Created the file`,
                });
              })
              .catch((err) => {
                console.log(err);
                return res.status(500).send({
                  message: `ERROR ${err}`,
                });
              });
          } else {
            return res.status(200).send({
              message: `Created the file`,
            });
          }
        } else {
          //OLD KEY, CHECK FOR CONFLICTS and IF SMALLEST
          console.log("OLD KEY");
          console.log("JSON OBJECTS", jsonObjects);

          console.log("CONTEXT", context);
          console.log(compareVectorClocks(context, jsonObjects[0].vectorClock));

          let smallest = true;
          for (let i = 0; i < jsonObjects.length; i++) {
            if (
              compareVectorClocks(context, jsonObjects[i].vectorClock) !==
              "second"
            ) {
              smallest = false;
              break;
            }
          }

          let conflict = false;
          for (let i = 0; i < jsonObjects.length; i++) {
            if (
              compareVectorClocks(context, jsonObjects[i].vectorClock) !==
                "first" &&
              compareVectorClocks(context, jsonObjects[i].vectorClock) !==
                "equal"
            ) {
              conflict = true;
              break;
            }
          }

          if (smallest) {
            return res.status(200).send({
              message: `Already have updated version of the file`,
            });
          } else if (conflict) {
            //CONFLICT, UPDATE VECTOR CLOCK AND STORE
            let newContext = { ...context };
            newContext[selfName] = newContext[selfName] + 1;

            saveJSONToFile(
              key,
              toStore,
              forwarded ? context : newContext,
              (err) => {
                if (err) {
                  // handle the error
                  console.error("Error saving JSON:", err);
                }

                if (!forwarded) {
                  let promises = [];

                  for (let i = 0; i < nodeSet.length; i++) {
                    if (nodeSet[i] === selfName) continue;
                    promises.push(
                      makeHttpRequest("POST", nodeSet[i], {
                        key: key,
                        data: data,
                        context: newContext,
                        forwarded: "true",
                      })
                    );
                  }

                  resolveIfAtLeastPromises(N - 1, W - 1, promises)
                    .then((result) => {
                      console.log(result);
                      return res.status(200).send({
                        message: `Updated the file (conflict)`,
                      });
                    })
                    .catch((err) => {
                      console.log(err);
                      return res.status(500).send({
                        message: `ERROR ${err}`,
                      });
                    });
                } else {
                  return res.status(200).send({
                    message: `Updated the file (conflict)`,
                  });
                }
              }
            );
          } else {
            //NO CONFLICT, UPDATE VECTOR CLOCK AND STORE
            let newContext = { ...context };
            newContext[selfName] = newContext[selfName] + 1;
            saveJSONToFile(
              key,
              toStore,
              forwarded ? context : newContext,
              (err) => {
                if (err) {
                  // handle the error
                  console.error("Error saving JSON:", err);
                }

                if (!forwarded) {
                  let promises = [];

                  for (let i = 0; i < nodeSet.length; i++) {
                    if (nodeSet[i] === selfName) continue;
                    promises.push(
                      makeHttpRequest("POST", nodeSet[i], {
                        key: key,
                        data: data,
                        context: newContext,
                        forwarded: "true",
                      })
                    );
                  }

                  resolveIfAtLeastPromises(N - 1, W - 1, promises)
                    .then((result) => {
                      console.log(result);
                      return res.status(200).send({
                        message: `Updated the file`,
                      });
                    })
                    .catch((err) => {
                      console.log(err);
                      return res.status(500).send({
                        message: `ERROR ${err}`,
                      });
                    });
                } else {
                  return res.status(200).send({
                    message: `Updated the file`,
                  });
                }
              }
            );
          }
        }
      });
    } else {
      // FORWARD TO THE ACTUAL COORDINATOR NODE
      console.log("FORWARDING TO NODE", nodeSet[0]);

      makeHttpRequest("POST", nodeSet[0], {
        key: key,
        data: data,
        context: context,
      }, true)
        .then((response) => {
          if (response.statusCode === 200) {
            return res.status(200).send({
              message: `Handled by node ${nodeSet[0]}, ${response.body.message}`,
            });
          } else {
            return res.status(500).send({
              message: `ERROR ${response.body.message}`,
            });
          }
        })
        .catch((err) => {
          return res.status(500).send({
            message: `ERROR ${err}`,
          });
        });
    }
  } catch (err) {
    return res.status(500).send({
      message: `ERROR ${err}`,
    });
  }
};

const getFile = async (req, res) => {
  try {
    const key = req.body.key;
    const forwarded =
      req.body.hasOwnProperty("forwarded") && req.body.forwarded === "true"
        ? true
        : false;

    const loadbalancer = new ConsistentHashing(
      Object.keys(nodeIPs),
      500,
      "md5"
    );
    const nodeSet = loadbalancer.getNodeset(key);

    if (nodeSet.length === 0) {
      return res.status(500).send({
        message: "No nodes in the ring!",
      });
    } else if (nodeSet.includes(selfName)) {
      // THIS IS THE COORDINATOR NODE
      let objectsRead = [];

      loadJSONFromFile(key, (err, jsonObjects) => {
        if (err) {
          console.error("Error loading JSON:", err);
          return res.status(500).send({
            message: "Key not found",
          });
        } else {
          objectsRead = [...jsonObjects, ...objectsRead];

          if (!forwarded) {
            let promises = [];

            console.log("FORWARDING TO NODES", nodeSet);

            for (let i = 0; i < nodeSet.length; i++) {
              if (nodeSet[i] === selfName) continue;
              promises.push(
                makeHttpRequest("GET", nodeSet[i], {
                  key: key,
                  forwarded: "true",
                })
              );
            }

            resolveIfAtLeastPromisesResult(N - 1, R - 1, promises)
              .then((results) => {
                results.forEach((result) => {
                  if (Array.isArray(result)) {
                    result.forEach((element) => {
                      try {
                        let clock = {
                          data: element.data,
                          vectorClock: element.vectorClock,
                        };
                        objectsRead = [clock, ...objectsRead];
                      } catch (e) {
                        //console.log(e)
                      }
                    });
                  }
                });

                objectsRead = filterDataNodes(objectsRead);

                return res.status(200).send(
                  JSON.stringify({
                    message: objectsRead,
                  })
                );
              })
              .catch((err) => {
                console.log(err);
                return res.status(500).send({
                  message: `ERROR ${err}`,
                });
              });
          } else {
            return res.status(200).send(
              JSON.stringify({
                message: objectsRead,
              })
            );
          }
        }
      });
    } else {
      // FORWARD TO THE ACTUAL COORDINATOR NODE
      console.log("FORWARDING TO NODE", nodeSet[0]);

      makeHttpRequest("GET", nodeSet[0], {
        key: key, 
      }, true).then((response) => {
          console.log("response is");
          console.log(response);
          console.log("response is");
          if (response.statusCode === 200) {
            return res.status(200).send({
              message: response.body.message,
            });
          } else {
            return res.status(500).send({
              message: `ERROR ${response.body.message}`,
            });
          }
        })
        .catch((err) => {
          return res.status(500).send({
            message: `ERROR ${err}`,
          });
        });
    }
  } catch (err) {
    return res.status(500).send({
      message: `ERROR ${err}`,
    });
  }
};

module.exports = {
  putFile,
  getFile,
};
